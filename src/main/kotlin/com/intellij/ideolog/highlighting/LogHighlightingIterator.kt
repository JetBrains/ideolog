package com.intellij.ideolog.highlighting

import com.intellij.ideolog.highlighting.settings.LogHighlightingAction
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.tree.IElementType
import java.awt.Color
import java.awt.Font
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.min

const val timeDifferenceToRed = 15000L

class LogHighlightingIterator(startOffset: Int, private val myEditor: Editor, val textGetter: () -> CharSequence, val colorGetter: () -> EditorColorsScheme) : HighlighterIterator {
  private val myText: CharSequence
    get() = textGetter()

  private val myColors: EditorColorsScheme
    get() = colorGetter()

  private val settingsStore = LogHighlightingSettingsStore.getInstance()
  private val myPatterns = settingsStore.myState.patterns.filter { it.enabled }.mapNotNull {
    try {
      Pattern.compile(it.pattern, Pattern.CASE_INSENSITIVE) to it
    } catch(e: PatternSyntaxException) {
      null
    }
  } // todo: notify user about invalid patterns

  private var parsedTokens = ArrayList<LogToken>()
  private val eventPieces = ArrayList<EventPiece>()
  private var currentEventLineRange: IntRange = 0..0
  private var eventPiecePointer = 0
  private var curEvent: CharSequence = ""

  init {
    val startLine = myEditor.document.getLineNumber(startOffset)

    currentEventLineRange = myEditor.document.ideologContext.getEvent(startLine)
    val prevEventLineRange = myEditor.document.ideologContext.getEvent(currentEventLineRange.first - 1)

    reparsePiecesLines(prevEventLineRange, currentEventLineRange)

    eventPieces.forEachIndexed { index, (offsetStart, offsetEnd) ->
      if (startOffset in offsetStart..offsetEnd) {
        eventPiecePointer = index
        return@forEachIndexed
      }
    }
  }

  private fun parseNextEvent() {
    var nextStartLine = currentEventLineRange.last + 1
    if (nextStartLine >= myEditor.document.lineCount) {
      eventPieces.clear()
      return
    }
    var nextStart = document.getLineStartOffset(nextStartLine)
    val foldingModel = myEditor.foldingModel
    foldingModel.getCollapsedRegionAtOffset(nextStart)?.let {
      if (!it.isExpanded) {
        nextStartLine = document.getLineNumber(it.endOffset) + 1
        if (nextStartLine >= myEditor.document.lineCount) {
          eventPieces.clear()
          return
        }
        nextStart = document.getLineStartOffset(nextStartLine)
      }
    }

    val nextLineRange = document.ideologContext.getEvent(nextStartLine)

    reparsePiecesLines(currentEventLineRange, nextLineRange)
    currentEventLineRange = nextLineRange

    eventPiecePointer = 0
  }

  private fun parsePreviousEvent() {
    if (currentEventLineRange.first == 0) {
      eventPieces.clear()
      return
    }

    val prevLineRange = document.ideologContext.getEvent(currentEventLineRange.first - 1)
    val prevPrevLineRange = document.ideologContext.getEvent(prevLineRange.first - 1)

    reparsePiecesLines(prevPrevLineRange, prevLineRange)
    currentEventLineRange = prevLineRange

    eventPiecePointer = eventPieces.size - 1
  }

    private fun linesSubSequence(lineRange: IntRange): CharSequence {
        if (lineRange.first < 0 || lineRange.last < lineRange.first)
            return ""

        val document = myEditor.document
        val startOffset = document.getLineStartOffset(lineRange.first)
        val endOffset = document.getLineEndOffset(min(document.lineCount - 1, lineRange.last))

        return document.immutableCharSequence.subSequence(startOffset, endOffset)
    }

  private fun reparsePiecesLines(prevEventLineRange: IntRange, lineRange: IntRange) {
    reparsePieces(linesSubSequence(prevEventLineRange), linesSubSequence(lineRange), document.getLineStartOffset(lineRange.first))
  }

  private fun reparsePieces(prevEvent: CharSequence, event: CharSequence, offset: Int) {
    Color.RGBtoHSB(myColors.defaultBackground.red, myColors.defaultBackground.green, myColors.defaultBackground.blue, myHsbVals)
    curEvent = event

    parsedTokens.clear()
    val fileFormat = detectLogFileFormat(myEditor)
    fileFormat.tokenize(prevEvent, parsedTokens)
    val prevTime = fileFormat.extractDate(parsedTokens)?.takeFrom(prevEvent)?.let { fileFormat.parseLogEventTimeSeconds(it) }

    eventPieces.clear()
    var lineForeground = myColors.defaultForeground
    var lineBackground = myColors.defaultBackground
    var bold = false
    var italic = false

    parsedTokens.clear()
    fileFormat.tokenize(event, parsedTokens)
    val currentTime = fileFormat.extractDate(parsedTokens)?.takeFrom(event)?.let { fileFormat.parseLogEventTimeSeconds(it) }

    val columnValues = parsedTokens.filter { !it.isSeparator }.map { it.takeFrom(event) }
    val numColumns = columnValues.size
    val highlightColumn = myEditor.getUserData(highlightingUserKey) ?: -1
    if (highlightColumn in 0 until numColumns) {
      val columnValue = columnValues[highlightColumn]
      lineBackground = getLineBackground(columnValue, myColors.defaultBackground) ?: lineBackground
    }
    val highlightingSet = myEditor.getUserData(highlightingSetUserKey) ?: emptySet()

    for ((pattern, info) in myPatterns) {
      if (!fileFormat.validateFormatUUID(info.formatId)) {
        continue
      }
      if (info.action == LogHighlightingAction.HIGHLIGHT_LINE) {
        when {
          info.captureGroup >= 0 -> {
            val it = columnValues.getOrNull(info.captureGroup) ?: break
            if (pattern.matcher(it).find()) {
              lineBackground = info.backgroundColor ?: lineBackground
              lineForeground = info.foregroundColor ?: lineForeground
              italic = info.italic
              bold = info.bold
              break
            }
          }

          else -> {
            for (it in columnValues) {
              if (pattern.matcher(it).find()) {
                lineBackground = info.backgroundColor ?: lineBackground
                lineForeground = info.foregroundColor ?: lineForeground
                italic = info.italic
                bold = info.bold
                break
              }
            }
          }
        }

      }
    }

    @Suppress("LoopToCallChain")
    for (word in highlightingSet) {
      if (event.contains(word)) {
        lineBackground = getLineBackground(word, myColors.defaultBackground) ?: lineBackground
      }
    }

    val partHighlighters = myPatterns.filter { it.second.action == LogHighlightingAction.HIGHLIGHT_MATCH }
    val valueHighlighters = myPatterns.filter { it.second.action == LogHighlightingAction.HIGHLIGHT_FIELD }

    var valueIndex = 0
    val timeIndex = fileFormat.getTimeFieldIndex()
    parsedTokens.filter{ !it.isSeparator }.forEachIndexed { captureGroup, token ->
      val value = token.takeFrom(event)
      var valueForeground = lineForeground
      var valueBackground = lineBackground
      var valueBold = bold
      var valueItalic = italic

      if (prevTime != null && currentTime != null && valueIndex == timeIndex && myEditor.getUserData(highlightTimeKey) == true) {
        val diff = abs(prevTime - currentTime)

        val diffLtd = min(timeDifferenceToRed, diff)
        valueBackground = Color(Color.HSBtoRGB((120 - diffLtd * 120 / timeDifferenceToRed) / 360.0f, if (myHsbVals[2] < 0.5f) 0.9f else 0.2f, if (myHsbVals[2] < 0.5f) 0.3f else 0.9f))
      }

      for ((pattern, info) in valueHighlighters) {
        if (!fileFormat.validateFormatUUID(info.formatId)
          || (info.captureGroup >= 0 && info.captureGroup != captureGroup)) {
          continue
        }

        if (pattern.matcher(value).find()) {
          valueForeground = info.foregroundColor ?: valueForeground
          valueBackground = info.backgroundColor ?: valueBackground
          valueBold = info.bold
          valueItalic = info.italic
          break
        }
      }

      // find all matches for every patttern
      val matchedPieces = partHighlighters.filter{
          (_, info) -> fileFormat.validateFormatUUID(info.formatId) && (info.captureGroup < 0 || info.captureGroup == captureGroup)
      }.flatMap { (pattern, info) ->
        pattern.toRegex().findAll(value).flatMap {
          it.groups.drop(1).mapNotNull { matchGroup ->
            matchGroup?.let {
              EventPiece(
                token.startOffset + offset + matchGroup.range.first,
                token.startOffset + offset + matchGroup.range.last + 1,
                TextAttributes(
                  info.foregroundColor ?: valueForeground,
                  info.backgroundColor ?: valueBackground,
                  null,
                  null,
                  getFont(info.bold, info.italic)
                ),
                false
              )

            }
          }
        }
      }.toMutableList()

      // add default background
      matchedPieces.add(
        EventPiece(
          token.startOffset + offset,
          token.endOffset + offset + 1,
          TextAttributes(valueForeground, valueBackground, null, null, getFont(valueBold, valueItalic)),
          false
        )
      )

      eventPieces.addAll(
        // cut lines to remove overlapping regions
        solveNestedLines(matchedPieces.mapIndexed { index, piece ->
          LineSegment(
            piece.offsetStart,
            piece.offsetEnd,
            index
          )
        }).map { lineSegment ->
          EventPiece(
            lineSegment.start,
            lineSegment.end,
            matchedPieces[lineSegment.id].textAttributes,
            matchedPieces[lineSegment.id].isSeparator,
          )
        }
      )

      valueIndex++
    }

    tryHighlightStacktrace(event, offset)
  }

  private fun tryHighlightStacktrace(event: CharSequence, eventOffset: Int) {
    val project = myEditor.project ?: return
    if (!settingsStore.myState.highlightLinks || !ApplicationManager.getApplication().isDispatchThread || event.indexOf('\n').let { it < 0 || it >= event.length - 1 })
      return

    ApplicationManager.getApplication().executeOnPooledThread {
      // Service init could be slow due to filters loading
      val service = LogHeavyFilterService.getInstance(project)
      ApplicationManager.getApplication().runReadAction {
        // could be slow and requires read action
        service.enqueueHeavyFiltering(myEditor, eventOffset, event)
      }
    }
  }

  private fun getFont(bold: Boolean, italic: Boolean): Int {
    return (if (bold) Font.BOLD else 0) + (if (italic) Font.ITALIC else 0)
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

  override fun getTokenType(): IElementType {
    return LOG_TOKEN_SEPARATOR
  }

  override fun advance() {
    if (eventPiecePointer < eventPieces.size - 1)
      eventPiecePointer++
    else
      parseNextEvent()
  }

  override fun retreat() {
    if (eventPiecePointer > 0)
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
      if (columnValue == null) {
        return null
      }
      val hash = abs(columnValue.hashCode()) % 360
      val bgHsl = Color.RGBtoHSB(defaultBackground.red, defaultBackground.green, defaultBackground.blue, myHsbVals)
      bgHsl[0] = hash / 360.0f
      bgHsl[1] = if (bgHsl[2] < 0.5f)
        1.0f
      else
        0.2f
      if (bgHsl[2] < 0.5f) bgHsl[2] = 0.3f
      return Color(Color.HSBtoRGB(bgHsl[0], bgHsl[1], bgHsl[2]))
    }
  }
}


data class LineSegment(val start: Int, val end: Int, val id: Int): Comparable<LineSegment> {
  override fun compareTo(other: LineSegment) = compareValuesBy(this, other, { it.start }, { it.end })
}

data class LineEvent(val x: Int, val l: Int, val id: Int,  val isOpening: Boolean)

/**
 * Solves nested line segments and returns a list of non-overlapping line segments.
 *
 * @param lines The list of line segments to solve.
 *              Each line segment consists of a start, end, and id.
 * @return The list of non-overlapping line segments.
 */
fun solveNestedLines(lines: List<LineSegment>): List<LineSegment> {
  val result : MutableList<LineSegment> = mutableListOf()
  val events : MutableList<LineEvent> = mutableListOf()
  lines.forEach {
    events.add(LineEvent(it.start, it.end - it.start, it.id, true))
    events.add(LineEvent(it.end, it.end - it.start, it.id, false))
  }

  events.sortWith(compareBy({ it.x }, { -it.l }))

  val stack = ArrayDeque(listOf<LineEvent>())
  var x = 0
  val closed = hashSetOf<Int>()
  events.forEach {
    when{
      it.isOpening -> {
        if (it.x > x) {
          if (stack.isNotEmpty()) {
            result.add(LineSegment(x, it.x, stack.last().id))
          }
          x = it.x
        }
        stack.addLast(it)
      }
      else -> {
        if (it.id == stack.last().id){
          result.add(LineSegment(x, it.x,  stack.last().id))
          x = it.x
        }
        closed.add(it.id)
        while (stack.isNotEmpty() && closed.contains(stack.last().id)){
          stack.removeLast()
        }
      }
    }
  }

  return result
}
