package com.intellij.ideolog.lex

import com.intellij.ideolog.highlighting.settings.LogParsingPattern
import com.intellij.ideolog.util.detectIdeologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.text.StringUtil
import java.text.DateFormat
import java.text.ParseException
import java.util.UUID
import java.util.regex.Pattern

data class LogToken(val startOffset: Int, var endOffset: Int, val isSeparator: Boolean) {
  fun takeFrom(rawMessage: CharSequence): CharSequence {
    return rawMessage.subSequence(startOffset, endOffset)
  }
}

class RegexLogParser(val uuid: UUID, val regex: Pattern, val lineRegex: Pattern, val otherParsingSettings: LogParsingPattern, val timeFormat: DateFormat)

private const val LINE_EVENT_START_TIMEOUT_MS: Long = 50

class LogFileFormat(val myRegexLogParser: RegexLogParser?) {
  fun isLineEventStart(line: CharSequence): Boolean {
    val lineRegex = myRegexLogParser?.lineRegex ?: return line.isNotEmpty() && !line[0].isWhitespace()
    return try {
      lineRegex.matcher(StringUtil.newBombedCharSequence(line, LINE_EVENT_START_TIMEOUT_MS)).find()
    }
    catch (_: ProcessCanceledException) {
      line.isNotEmpty() && !line[0].isWhitespace()
    }
  }

  fun getTimeFieldIndex(): Int {
    return myRegexLogParser?.otherParsingSettings?.timeColumnId ?: 0
  }

  fun tokenize(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean = false) {
    if(myRegexLogParser == null) {
      LogFileLexer.lexPlainLog(event, output, onlyValues)
    } else {
      LogFileLexer.lexRegex(event, output, onlyValues, myRegexLogParser)
    }
  }

  fun extractDate(tokens: List<LogToken>): LogToken? {
    val idx = myRegexLogParser?.otherParsingSettings?.timeColumnId ?: return null
    if(tokens.size > idx)
      return tokens.asSequence().filter { !it.isSeparator }.elementAtOrNull(idx)
    return null
  }

  fun extractSeverity(tokens: List<LogToken>): LogToken? {
    val idx = myRegexLogParser?.otherParsingSettings?.severityColumnId ?: return null
    if(tokens.size > idx)
      return tokens.asSequence().filter { !it.isSeparator }.elementAtOrNull(idx)
    return null
  }

  fun extractCategory(tokens: List<LogToken>): LogToken? {
    val idx = myRegexLogParser?.otherParsingSettings?.categoryColumnId ?: return null
    if(tokens.size > idx)
      return tokens.asSequence().filter { !it.isSeparator }.elementAtOrNull(idx)
    return null
  }

  fun extractMessage(tokens: List<LogToken>): LogToken {
    return tokens.last { !it.isSeparator }
  }

  fun validateFormatUUID(uuid: UUID?): Boolean =
    uuid == null || myRegexLogParser?.uuid == uuid


  fun parseLogEventTimeSeconds(time: CharSequence): Long? {
    return myRegexLogParser?.let {
      try {
        return@let it.timeFormat.parse(time.toString()).time
      } catch (_: ParseException) {
        // silently ignore it
      } catch (_: NumberFormatException) {

      } catch (_: ArrayIndexOutOfBoundsException) {
        // apparently this one is also randomly thrown by parsing
      }
      return@let null
    }
  }
}

fun detectLogFileFormat(editor: Editor): LogFileFormat = detectIdeologContext(editor).detectLogFileFormat()

fun detectLogFileFormat(editor: Editor, offset: Int): LogFileFormat = detectIdeologContext(editor).detectLogFileFormat(offset)
