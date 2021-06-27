package com.intellij.ideolog.util

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import gnu.trove.TIntIntHashMap
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

private val documentContextKey = Key.create<IdeologDocumentContext>("IdeologDocumentContext")

val Document.ideologContext : IdeologDocumentContext
  get() = getUserData(documentContextKey) ?: run {
    putUserData(documentContextKey, IdeologDocumentContext(this))
    getUserData(documentContextKey)!! // get again in case of multi-threaded writes (UDH is thread-safe)
  }

class IdeologDocumentContext(val document: Document) {
  private val eventStartLines = TIntIntHashMap()
  private val eventEndLines = TIntIntHashMap()

  private val eventParsingLock = Any()

  val hiddenItems = HashSet<Pair<Int, String>>()
  val hiddenSubstrings = HashSet<String>()
  val whitelistedSubstrings = HashSet<String>()
  val whitelistedItems = HashSet<Pair<Int, String>>()
  var hideLinesAbove: Int = -1
  var hideLinesBelow: Int = Int.MAX_VALUE

  private var format: LogFileFormat? = null

  fun clear() {
    synchronized(eventParsingLock) {
      eventStartLines.clear()
      eventEndLines.clear()
    }

    format = null
  }

  fun detectLogFileFormat(): LogFileFormat {
    val format2 = format
    if (format2 != null) return format2

    val regexMatchers = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.mapNotNull {
      if (!it.enabled)
        return@mapNotNull null

      try {
        return@mapNotNull RegexLogParser(Pattern.compile(it.pattern, Pattern.DOTALL), Pattern.compile(it.lineStartPattern), it, SimpleDateFormat(it.timePattern))
      } catch(e: PatternSyntaxException){
        println(e)
        return@mapNotNull null
      }
    }

    val doc = document.charsSequence
    val firstLines = doc.lineSequence().take(25)
    val sumByMatcher = regexMatchers.map { it to firstLines.count { line -> it.regex.matcher(line).find() } }

    return LogFileFormat(sumByMatcher.filter { it.second > 5 }.maxByOrNull { it.second }?.first).also { format = it }
  }

  /**
   * Returns line range for the event on given line
   */
  fun getEvent(atLine: Int): IntRange {
    if (atLine < 0)
      return -1..-1

    synchronized(eventParsingLock) {
      val startLine = getEventStartLine(atLine)
      val endLine = getEventEndLine(atLine)

      return startLine..endLine
    }
  }

  private fun lineCharSequence(line: Int) =
    document.immutableCharSequence.subSequence(document.getLineStartOffset(line), document.getLineEndOffset(line))

  private fun getEventEndLine(atLine: Int): Int {
    if (eventEndLines.containsKey(atLine))
      return eventEndLines[atLine]

    val format = detectLogFileFormat()

    val lineCount = document.lineCount

    var currentLine = atLine

    fun updateCache(highLine: Int, value: Int) {
      for (i in atLine .. highLine) {
        eventEndLines.put(i, value)
      }
    }

    while (currentLine < lineCount - 1 && !format.isLineEventStart(lineCharSequence(currentLine + 1))) {
      currentLine++

      if (eventEndLines.containsKey(currentLine)) {
        val result = eventEndLines[currentLine]
        updateCache(currentLine, result)
        return result
      }
    }

    updateCache(currentLine, currentLine)
    return currentLine
  }

  private fun getEventStartLine(atLine: Int): Int {
    if (eventStartLines.containsKey(atLine))
      return eventStartLines[atLine]

    val format = detectLogFileFormat()

    fun updateCache(lowLine: Int, targetLine: Int) {
      for (i in lowLine..atLine)
        eventStartLines.put(i, targetLine)
    }

    var currentLine = atLine
    while (currentLine > 0 && !format.isLineEventStart(lineCharSequence(currentLine))) {
      currentLine--
      if (eventStartLines.containsKey(currentLine)) {
        val result = eventStartLines[currentLine]
        updateCache(currentLine, result)
        return result
      }
    }

    updateCache(currentLine, currentLine)
    return currentLine
  }
}

fun Editor.getSelectedText(): CharSequence? {
  val selectionModel = this.selectionModel
  var selectionStart = selectionModel.selectionStart
  var selectionEnd = selectionModel.selectionEnd


  if (selectionStart == selectionEnd) {
    val doc = this.document.charsSequence

    while (selectionStart > 0 && doc[selectionStart - 1].isLetterOrDigit())
      selectionStart--

    while (selectionEnd < doc.length && doc[selectionEnd].isLetterOrDigit())
      selectionEnd++
  }

  if (selectionEnd - selectionStart > 100 || selectionEnd == selectionStart)
    return null

  return this.document.getText(TextRange(selectionStart, selectionEnd))
}

data class GoToActionContext(val event: LogEvent,
                             val editor: Editor,
                             val foldingModel: FoldingModel,
                             val project: Project,
                             val psiFile: PsiFile)

fun AnActionEvent.getGoToActionContext(): GoToActionContext? {
  val editor = this.dataContext.getData(CommonDataKeys.EDITOR) ?: return null
  val psiFile = this.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return null
  val project = this.dataContext.getData(CommonDataKeys.PROJECT) ?: return null
  if (psiFile.fileType != LogFileType) return null

  val foldingModel = editor.foldingModel
  val event = LogEvent.fromEditor(editor, editor.caretModel.offset)
  return GoToActionContext(event, editor, foldingModel, project, psiFile)
}
