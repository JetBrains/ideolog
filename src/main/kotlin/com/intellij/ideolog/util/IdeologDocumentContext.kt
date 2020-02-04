package com.intellij.ideolog.util

import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
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
    return format ?: run {
      val regexMatchers = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.mapNotNull {
        if (!it.enabled)
          return@mapNotNull null

        try {
          return@mapNotNull RegexLogParser(Pattern.compile(it.pattern, Pattern.DOTALL), Pattern.compile(it.lineStartPattern), it, SimpleDateFormat(it.timePattern))
        } catch(e: PatternSyntaxException){
          return@mapNotNull null
        }
      }

      val doc = document.charsSequence
      val firstLines = doc.lineSequence().take(25)
      val sumByMatcher = regexMatchers.map { it to firstLines.count { line -> it.regex.matcher(line).find() } }

      LogFileFormat(sumByMatcher.filter { it.second > 5 }.maxBy { it.second }?.first)
    }.also { format = it }
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
