package com.intellij.ideolog.lex

import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.highlighting.settings.LogParsingPattern
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

data class LogToken(val startOffset: Int, var endOffset: Int, val isSeparator: Boolean) {
  fun takeFrom(rawMessage: CharSequence): CharSequence {
    return rawMessage.subSequence(startOffset, endOffset)
  }
}

class RegexLogParser(val regex: Pattern, val lineRegex: Pattern, val otherParsingSettings: LogParsingPattern, val timeFormat: DateFormat)

class LogFileFormat(val myRegexLogParser: RegexLogParser?) {
  fun isLineEventStart(line: CharSequence): Boolean {
    return myRegexLogParser?.lineRegex?.matcher(line)?.find() ?: line.isNotEmpty() && !line[0].isWhitespace()
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
    if(tokens.size > idx + 1)
      return tokens.asSequence().filter { !it.isSeparator }.elementAt(idx)
    return null
  }

  fun extractSeverity(tokens: List<LogToken>): LogToken? {
    val idx = myRegexLogParser?.otherParsingSettings?.severityColumnId ?: return null
    if(tokens.size > idx + 1)
      return tokens.asSequence().filter { !it.isSeparator }.elementAt(idx)
    return null
  }

  fun extractCategory(tokens: List<LogToken>): LogToken? {
    val idx = myRegexLogParser?.otherParsingSettings?.categoryColumnId ?: return null
    if(tokens.size > idx + 1)
      return tokens.asSequence().filter { !it.isSeparator }.elementAt(idx)
    return null
  }

  fun extractMessage(tokens: List<LogToken>): LogToken {
    return tokens.last { !it.isSeparator }
  }

  fun parseLogEventTimeSeconds(time: CharSequence): Long? {
    return myRegexLogParser?.let {
      try {
        return@let it.timeFormat.parse(time.toString()).time
      } catch (e: ParseException) {
        // silently ignore it
      } catch (e: NumberFormatException) {

      }
      return@let null
    }
  }
}

val logFormatKey = Key.create<LogFileFormat>("LogFile.Format")

fun detectLogFileFormat(editor: Editor): LogFileFormat {
  val existingKey = editor.getUserData(logFormatKey)
  if (existingKey != null)
    return existingKey

  val regexMatchers = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.mapNotNull {
    if (!it.enabled)
      return@mapNotNull null

    try {
      return@mapNotNull RegexLogParser(Pattern.compile(it.pattern, Pattern.DOTALL), Pattern.compile(it.lineStartPattern), it, SimpleDateFormat(it.timePattern))
    } catch(e: PatternSyntaxException){
      return@mapNotNull null
    }
  }

  val doc = editor.document.charsSequence
  val firstLines = doc.lineSequence().take(25)
  val sumByMatcher = regexMatchers.map { it to firstLines.count { line -> it.regex.matcher(line).find() } }

  val result = LogFileFormat(sumByMatcher.filter { it.second > 5 }.maxBy { it.second }?.first)

  editor.putUserData(logFormatKey, result)

  return result
}
