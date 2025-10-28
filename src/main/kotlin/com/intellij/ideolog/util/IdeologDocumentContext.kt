package com.intellij.ideolog.util

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.largeFile.IdeologLargeFileDocumentContext
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.statistics.IdeologUsagesCollector
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

private val documentContextKey = Key.create<IdeologDocumentContext>("IdeologDocumentContext")

val Document.ideologContext: IdeologDocumentContext
  get() = getUserData(documentContextKey) ?: run {
    putUserData(documentContextKey, IdeologDocumentContext(this))
    getUserData(documentContextKey)!! // get again in case of multithreading writes (UDH is thread-safe)
  }

open class IdeologDocumentContext(val document: Document, private val cache: EventCache? = EventCache()) {
  companion object {
    private const val NUMBER_FIRST_LINES = 25
    private const val MIN_FORMAT_MATCHES = 1
    private const val INTERRUPT_AFTER_NS = 500 * 1_000_000
  }

  open val numberFirstLines: Int
    get() = NUMBER_FIRST_LINES

  data class EventCache(
    val eventStartLines: HashMap<Int, Int> = HashMap(),
    val eventEndLines: HashMap<Int, Int> = HashMap(),
  ) {
    fun clear() {
      eventStartLines.clear()
      eventEndLines.clear()
    }
  }

  private val eventParsingLock = Any()

  val hiddenItems: HashSet<Pair<Int, String>> = HashSet()
  val hiddenSubstrings: HashSet<String> = HashSet()
  val whitelistedSubstrings: HashSet<String> = HashSet()
  val whitelistedItems: HashSet<Pair<Int, String>> = HashSet()
  var hideLinesAbove: Int = -1
  var hideLinesBelow: Int = Int.MAX_VALUE

  private var format: LogFileFormat? = null

  private val needLogging = this::class == IdeologDocumentContext::class || this::class == IdeologLargeFileDocumentContext::class

  open fun clear() {
    synchronized(eventParsingLock) {
      cache?.clear()
    }

    if (format?.myRegexLogParser == null) {
      format = null
    }
  }

  open fun detectLogFileFormat(startOffset: Int = 0): LogFileFormat {
    val logFileOffset = if (this::class == IdeologDocumentContext::class) 0 else startOffset
    if (cache != null || logFileOffset == 0) {
      val currentFormat = format
      if (currentFormat?.isEnabled() == true) {
        return currentFormat
      }
      clear()
    }

    val regexMatchers = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.mapNotNull {
      if (!it.enabled)
        return@mapNotNull null

      try {
        return@mapNotNull RegexLogParser(
          it.uuid,
          Pattern.compile(it.pattern, Pattern.DOTALL),
          Pattern.compile(it.lineStartPattern),
          it,
          SimpleDateFormat(it.timePattern)
        )
      } catch (e: PatternSyntaxException) {
        thisLogger().info(e)
        return@mapNotNull null
      }
    }

    val documentAfterOffset = document.charsSequence.drop(logFileOffset)
    val firstLinesAfterOffset = documentAfterOffset.lineSequence().take(numberFirstLines)

    val detectedLogFormat = getLogFileFormat(firstLinesAfterOffset, regexMatchers)
    format = detectedLogFormat
    if (needLogging) {
      IdeologUsagesCollector.logDetectedLogFormat(format)
    }
    return detectedLogFormat
  }

  open fun getLogFileFormat(fileLines: Sequence<String>, regexMatchers: List<RegexLogParser>): LogFileFormat {
    val startTime = System.nanoTime()
    val sumByMatcher = regexMatchers.map {
      it to fileLines.count { line ->
        System.nanoTime() - startTime < INTERRUPT_AFTER_NS && it.regex.matcher(line).find()
      }
    }
    return LogFileFormat(sumByMatcher.filter { it.second >= MIN_FORMAT_MATCHES }.maxByOrNull { it.second }?.first)
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

  protected fun lineCharSequence(line: Int): CharSequence =
    document.immutableCharSequence.subSequence(document.getLineStartOffset(line), document.getLineEndOffset(line))

  open fun isLineEventStart(atLine: Int): Boolean {
    val format = detectLogFileFormat(document.getLineStartOffset(atLine))
    return format.isLineEventStart(lineCharSequence(atLine))
  }

  private fun getEventEndLine(atLine: Int): Int {
    cache?.eventEndLines?.get(atLine)?.let { return it }

    val lineCount = document.lineCount

    var currentLine = atLine

    fun updateCache(highLine: Int, value: Int) {
      for (i in atLine..highLine) {
        cache?.eventEndLines?.put(i, value)
      }
    }

    while (currentLine < lineCount - 1 && !isLineEventStart(currentLine + 1)) {
      currentLine++

      cache?.eventEndLines?.get(currentLine)?.also { updateCache(currentLine, it) }?.let { return it }
    }

    updateCache(currentLine, currentLine)
    return currentLine
  }

  private fun getEventStartLine(atLine: Int): Int {
    cache?.eventStartLines?.get(atLine)?.let { return it }

    fun updateCache(lowLine: Int, targetLine: Int) {
      for (i in lowLine..atLine)
        cache?.eventStartLines?.put(i, targetLine)
    }

    var currentLine = atLine
    while (currentLine > 0 && !isLineEventStart(currentLine)) {
      currentLine--
      cache?.eventStartLines?.get(currentLine)?.also { updateCache(currentLine, it) }?.let { return it }
    }

    updateCache(currentLine, currentLine)
    return currentLine
  }
}

private fun LogFileFormat.isEnabled(): Boolean {
  return myRegexLogParser == null || LogHighlightingSettingsStore.getInstance().myState.parsingPatterns
    .firstOrNull { pattern -> pattern.uuid == this.myRegexLogParser.uuid }?.enabled == true
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

data class GoToActionContext(
  val event: LogEvent,
  val editor: Editor,
  val foldingModel: FoldingModel,
  val project: Project,
  val psiFile: PsiFile
)

fun AnActionEvent.getGoToActionContext(): GoToActionContext? {
  val editor = this.dataContext.getData(CommonDataKeys.EDITOR) ?: return null
  val psiFile = this.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return null
  val project = this.dataContext.getData(CommonDataKeys.PROJECT) ?: return null
  if (psiFile.fileType != LogFileType) return null

  val foldingModel = editor.foldingModel
  val event = LogEvent.fromEditor(editor, editor.caretModel.offset)
  return GoToActionContext(event, editor, foldingModel, project, psiFile)
}
