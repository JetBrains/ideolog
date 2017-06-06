package com.intellij.ideolog.highlighting

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.FilterMixin
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.ideolog.fileType.LogLanguage
import com.intellij.ideolog.highlighting.settings.LogHighlightingAction
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import java.awt.Color
import java.awt.Font
import java.util.*
import java.util.regex.Pattern

val LOG_TOKEN_SEPARATOR = IElementType("LOG_TOKEN_SEPARATOR", LogLanguage)

internal val highlightingUserKey = Key.create<Int>("JetLog.HighlightColumn")
internal val highlightingSetUserKey = Key.create<HashSet<String>>("JetLog.HighlightSet")
val highlightTimeKey = Key.create<Boolean>("JetLog.HighlightTime")

class LogTokenElementType(val column: Int): IElementType("LOG_TOKEN_VALUE_$column", LogLanguage, false)
class LogFileEditorHighlighterProvider: EditorHighlighterProvider {

  override fun getEditorHighlighter(project: Project?, fileType: FileType, virtualFile: VirtualFile?, colors: EditorColorsScheme): EditorHighlighter {
    return LogEditorHighlighter(colors)
  }
}

class LogEditorHighlighter(colors: EditorColorsScheme) : EditorHighlighter {
  private var myColors: EditorColorsScheme = colors
  private var myText: CharSequence = ""
  private var myEditor: HighlighterClient? = null
  private var myFilters: List<Filter> = emptyList()

  override fun createIterator(startOffset: Int): HighlighterIterator {
    if(myEditor == null)
      return EmptyEditorHighlighter(TextAttributes(null, null, null, null, 0)).createIterator(startOffset)


    return LogHighlightingIterator(startOffset, myEditor as Editor, { myText }, { myColors }, myFilters)
  }

  override fun setText(text: CharSequence) {
    myText = text
  }

  override fun setEditor(editor: HighlighterClient) {
    myEditor = editor
    val project = editor.project ?: return
    myFilters = ConsoleFilterProvider.FILTER_PROVIDERS.extensions.flatMap { it.getDefaultFilters(project).asIterable() }
  }

  override fun setColorScheme(scheme: EditorColorsScheme) {
    myColors = scheme
  }

  override fun beforeDocumentChange(event: DocumentEvent?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun documentChanged(event: DocumentEvent) {
    myText = event.document.charsSequence
    myEditor?.repaint(0, myText.length)
  }
}

val markupHighlightedExceptionsKey = Key.create<HashSet<Int>>("Log.ParsedExceptions")
val timePattern = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d)")!!
val timeDifferenceToRed = 15

fun parseLogEventTimeSeconds(s:CharSequence) : Int?
{
  val matcher = timePattern.matcher(s)
  matcher.find() || return -1
  return matcher.group(1).toInt() * 3600 + matcher.group(2).toInt() * 60 + matcher.group(3).toInt()
}

private var highlightingStacktrace = false // todo: ewww, globals!
class LogHighlightingIterator(private val startOffset: Int, val myEditor: Editor, val textGetter: () -> CharSequence, val colorGetter: () -> EditorColorsScheme, val filters: List<Filter>) : HighlighterIterator {
  val myText: CharSequence
    get() = textGetter()

  val myColors: EditorColorsScheme
    get() = colorGetter()

  val myPatterns = LogHighlightingSettingsStore.getInstance().myState.patterns.filter { it.enabled }.map { Pattern.compile(it.pattern, Pattern.CASE_INSENSITIVE) to it } // todo: detect invalid patterns

  private var parsedTokens = ArrayList<LogToken>()
  private val eventPieces = ArrayList<EventPiece>()
  private var eventPiecePointer = 0
  private var curEvent: CharSequence = ""

  init {
    val (event, offset) = LogParsingUtils.getEvent(myEditor.shouldFindTrueEventStart(), detectLogFileFormat(myEditor), myEditor.document, myText, startOffset)
    val (prevEvent, _) = LogParsingUtils.getEvent(myEditor, offset - 1)

    reparsePieces(prevEvent, event, offset)

    eventPieces.forEachIndexed { index, (offsetStart, offsetEnd) ->
      if(startOffset in offsetStart..offsetEnd) {
        eventPiecePointer = index
        return@forEachIndexed
      }
    }
  }

  fun parseNextEvent() {
    val lastStart = eventPieces[0].offsetStart
    var nextStart = eventPieces.last().offsetEnd + 1
    if(nextStart >= myText.length) {
      eventPieces.clear()
      return
    }
    val foldingModel = myEditor.foldingModel
    foldingModel.getCollapsedRegionAtOffset(nextStart)?.let {
      if(!it.isExpanded)
        nextStart = it.endOffset
    }


    val (event, offset) = LogParsingUtils.getEvent(myEditor.shouldFindTrueEventStart(), detectLogFileFormat(myEditor), myEditor.document, myText, nextStart)

    if(offset == lastStart) {
      eventPieces.clear()
      return
    }

    reparsePieces(curEvent, event, offset)

    eventPiecePointer = 0
  }

  fun parsePreviousEvent() {
    val prevEnd = eventPieces[0].offsetStart - 1
    if(prevEnd < 0) return

    val (event, offset) = LogParsingUtils.getEvent(myEditor.shouldFindTrueEventStart(), detectLogFileFormat(myEditor), myEditor.document, myText, prevEnd)
    val (prevEvent, _) = LogParsingUtils.getEvent(myEditor, offset - 1)

    reparsePieces(prevEvent, event, offset)

    eventPiecePointer = eventPieces.size - 1
  }

  private fun reparsePieces(prevEvent: CharSequence, event: CharSequence, offset: Int) {
    Color.RGBtoHSB(myColors.defaultBackground.red, myColors.defaultBackground.green, myColors.defaultBackground.blue, myHsbVals)
    curEvent = event

    parsedTokens.clear()
    val fileType = detectLogFileFormat(myEditor)
    fileType.tokenize(prevEvent, parsedTokens)
    val prevTime = detectLogFileFormat(myEditor).extractDate(parsedTokens)?.takeFrom(prevEvent)?.let { parseLogEventTimeSeconds(it) }

    eventPieces.clear()
    var lineForeground = myColors.defaultForeground
    var lineBackground = myColors.defaultBackground
    var bold = false
    var italic = false

    parsedTokens.clear()
    fileType.tokenize(event, parsedTokens)
    val currentTime = fileType.extractDate(parsedTokens)?.takeFrom(event)?.let { parseLogEventTimeSeconds(it) }

    val columnValues = parsedTokens.filter { !it.isSeparator }.map { it.takeFrom(event) }
    val numColumns = columnValues.size
    val highlightColumn = myEditor.getUserData(highlightingUserKey) ?: -1
    if(highlightColumn in 0..(numColumns - 1)) {
      val columnValue = columnValues[highlightColumn]
      lineBackground = Companion.getLineBackground(columnValue, myColors.defaultBackground) ?: lineBackground
    }
    val highlightingSet = myEditor.getUserData(highlightingSetUserKey) ?: emptySet<String>()

    for((pattern, info) in myPatterns) {
      if(info.action == LogHighlightingAction.HIGHLIGHT_LINE) {
        for (it in columnValues) {
          if(pattern.matcher(it).find()) {
            lineBackground = info.backgroundColor ?: lineBackground
            lineForeground = info.foregroundColor ?: lineForeground
            italic = info.italic
            bold = info.bold
            break
          }
        }
      }
    }

    @Suppress("LoopToCallChain")
    for(word in highlightingSet) {
      if(event.contains(word)) {
        lineBackground = Companion.getLineBackground(word, myColors.defaultBackground) ?: lineBackground
      }
    }

    val partHighlighters = myPatterns.filter { it.second.action == LogHighlightingAction.HIGHLIGHT_MATCH }
    val valueHighlighters = myPatterns.filter { it.second.action == LogHighlightingAction.HIGHLIGHT_FIELD }

    val currentPieces = ArrayList<EventPiece>()

    var valueIndex = 0
    val timeIndex = fileType.getTimeFieldIndex()
    parsedTokens.forEachIndexed { _, token ->
      val value = token.takeFrom(event)
      var valueForeground = lineForeground
      var valueBackground = lineBackground
      var valueBold = bold
      var valueItalic = italic

      if(!token.isSeparator && prevTime != null && currentTime != null && valueIndex == timeIndex && myEditor.getUserData(highlightTimeKey) ?: false) {
        val diff = Math.abs(prevTime - currentTime)

        val diffLtd = Math.min(timeDifferenceToRed, diff)
        valueBackground = Color(Color.HSBtoRGB((120 - diffLtd * 120 / timeDifferenceToRed) / 360.0f, if(myHsbVals[2] < 0.5f) 0.9f else 0.2f, if(myHsbVals[2] < 0.5f) 0.3f else 0.9f))
      }

      if(!token.isSeparator) {
        for ((pattern, info) in valueHighlighters) {
          if (pattern.matcher(value).find()) {
            valueForeground = info.foregroundColor ?: valueForeground
            valueBackground = info.backgroundColor ?: valueBackground
            valueBold = info.bold
            valueItalic = info.italic
            break
          }
        }
      }

      val newlineOffset = value.indexOf('\n')
      currentPieces.clear()
      if(newlineOffset > 0 && newlineOffset < value.length - 1) {
        currentPieces.add(EventPiece(token.startOffset + offset, token.startOffset + newlineOffset + offset, TextAttributes(valueForeground, valueBackground, null, null, getFont(valueBold, valueItalic)), token.isSeparator))
        currentPieces.add(EventPiece(token.startOffset + newlineOffset + offset, token.endOffset + offset, TextAttributes(valueForeground, valueBackground, null, null, 0), token.isSeparator))
      } else
        currentPieces.add(EventPiece(token.startOffset + offset, token.endOffset + offset, TextAttributes(valueForeground, valueBackground, null, null, getFont(valueBold, valueItalic)), token.isSeparator)) // todo: lexeme type?

      if(!token.isSeparator) {
        for ((pattern, _) in partHighlighters) {
          currentPieces.forEachIndexed { _, (offsetStart, offsetEnd) ->
            val matcher = pattern.matcher(myText.subSequence(offsetStart, offsetEnd))
            while (matcher.find()) {
              // todo: match highlighting
            }
          }
        }
      }

      if(!token.isSeparator)
        valueIndex++

      eventPieces.addAll(currentPieces)
    }

    tryHighlightStacktrace(event, offset)
  }


  private fun tryHighlightStacktrace(event: CharSequence, eventOffset: Int) {
    if(highlightingStacktrace || !ApplicationManager.getApplication().isDispatchThread || event.indexOf('\n').let { it < 0 || it >= event.length - 1 })
      return
    highlightingStacktrace = true
    fun offsetVisible(offset: Int) : Boolean {
      val lineNumber = document.getLineNumber(offset)
      val vp = myEditor.logicalToVisualPosition(LogicalPosition(lineNumber, 0))
      return myEditor.scrollingModel.visibleAreaOnScrollingFinished.contains(myEditor.visualPositionToXY(vp))
    }
    if(!offsetVisible(eventOffset) && !offsetVisible(eventOffset + event.length)) {
      highlightingStacktrace = false
      return
    }
    highlightingStacktrace = false

    val markupModel = myEditor.markupModel

    val set = markupModel.getUserData(markupHighlightedExceptionsKey) ?: HashSet()

    if(set.contains(eventOffset))
      return

    set.add(eventOffset)
    markupModel.putUserData(markupHighlightedExceptionsKey, set)

    val hyperlinkSupport = EditorHyperlinkSupport(myEditor, myEditor.project!!)

    fun consumeResult(result: Filter.Result?, addOffset: Boolean) {
      result ?: return
      val extraOffset = if(addOffset) eventOffset else 0
      result.resultItems.forEach {
        val hyperlinkInfo = it.getHyperlinkInfo()
        if(hyperlinkInfo != null)
          hyperlinkSupport.createHyperlink(it.getHighlightStartOffset() + extraOffset, it.getHighlightEndOffset() + extraOffset, it.getHighlightAttributes(), hyperlinkInfo)
        else
          markupModel.addRangeHighlighter(it.getHighlightStartOffset() + extraOffset, it.getHighlightEndOffset() + extraOffset, it.highlighterLayer, it.getHighlightAttributes(), HighlighterTargetArea.EXACT_RANGE)
      }
    }

    val lines = event.split('\n')
    var offset = 0
    val subDoc = DocumentImpl(event)
    lines.forEachIndexed { index, line ->
      offset += line.length
      filters.forEach { filter ->
        if(filter is FilterMixin && filter.shouldRunHeavy()) {
          filter.applyHeavyFilter(subDoc, 0, index) {
            consumeResult(it, true)
          }
        } else
          consumeResult(filter.applyFilter(line, eventOffset + offset), false)
      }
      offset += 1
    }
  }

  fun getFont(bold: Boolean, italic: Boolean): Int {
    return (if(bold) Font.BOLD else 0) + (if(italic) Font.ITALIC else 0)
  }

  override fun getTextAttributes(): TextAttributes {
    return eventPieces[eventPiecePointer].textAttributes
  }

  override fun getStart(): Int {
    return eventPieces[eventPiecePointer].offsetStart
  }

  override fun getEnd(): Int {
    return eventPieces[eventPiecePointer].offsetEnd
  }

  override fun getTokenType(): IElementType? {
    return LOG_TOKEN_SEPARATOR
  }

  override fun advance() {
    if(eventPiecePointer < eventPieces.size - 1)
      eventPiecePointer++
    else
      parseNextEvent()
  }

  override fun retreat() {
    if(eventPiecePointer > 0)
      eventPiecePointer--
    else
      parsePreviousEvent()
  }

  override fun atEnd(): Boolean {
    return eventPieces.isEmpty()
  }

  override fun getDocument(): Document {
    return myEditor.document
  }

  private data class EventPiece(val offsetStart: Int, val offsetEnd: Int, val textAttributes: TextAttributes, val isSeparator: Boolean)
  companion object {
    val myHsbVals = FloatArray(3)
    fun getLineBackground(columnValue: CharSequence?, defaultBackground: Color): Color? {
      if(columnValue == null) {
        return null
      }
      val hash = Math.abs(columnValue.hashCode()) % 360
      val bgHsl = Color.RGBtoHSB(defaultBackground.red, defaultBackground.green, defaultBackground.blue, myHsbVals)
      bgHsl[0] = hash / 360.0f
      bgHsl[1] = if(bgHsl[2] < 0.5f)
        1.0f
      else
        0.2f
      if(bgHsl[2] < 0.5f) bgHsl[2] = 0.3f
      return Color(Color.HSBtoRGB(bgHsl[0], bgHsl[1], bgHsl[2]))
    }
  }
}

