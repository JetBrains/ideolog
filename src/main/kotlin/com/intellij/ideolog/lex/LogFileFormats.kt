package com.intellij.ideolog.lex

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import java.util.regex.Pattern

data class LogToken(val startOffset: Int, var endOffset: Int, val isSeparator: Boolean) {
  fun takeFrom(rawMessage: CharSequence): CharSequence {
    return rawMessage.subSequence(startOffset, endOffset)
  }
}

enum class LogFileFormat {
  PIPE_SEPARATED,
  YOUTRACK,
  PLAIN;

  fun isLineEventStart(line: CharSequence): Boolean {
    return when (this) {
      LogFileFormat.PIPE_SEPARATED -> line.isNotEmpty() && line[0].isDigit() && pipePattern.matcher(line).find()
      LogFileFormat.YOUTRACK -> line.isNotEmpty() && line[0] == '[' && youTrackPattern.matcher(line).find()
      LogFileFormat.PLAIN -> line.isNotEmpty() && !line[0].isWhitespace()
    }
  }

  fun getTimeFieldIndex(): Int {
    return when (this) {
      LogFileFormat.PIPE_SEPARATED -> 0
      LogFileFormat.YOUTRACK -> 0
      LogFileFormat.PLAIN -> 0
    }
  }

  fun tokenize(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean = false) {
    when (this) {
      LogFileFormat.PIPE_SEPARATED -> LogFileLexer.lexPipeLine(event, output, onlyValues)
      LogFileFormat.YOUTRACK -> LogFileLexer.lexYoutrackLine(event, output, onlyValues)
      LogFileFormat.PLAIN -> LogFileLexer.lexPlainLog(event, output, onlyValues)
    }
  }

  fun extractDate(tokens: List<LogToken>): LogToken? {
    return when (this) {
      LogFileFormat.PIPE_SEPARATED -> if (tokens.size > 1) tokens[0] else null
      LogFileFormat.YOUTRACK -> if ((tokens.size > 1) && (!tokens[0].isSeparator)) tokens[0] else {
        if ((tokens.size > 2) && (tokens[0].isSeparator) && (!tokens[1].isSeparator)) tokens[1] else null
      }
      LogFileFormat.PLAIN -> if (tokens.size > 1) tokens[0] else null
    }
  }

  fun extractSeverity(tokens: List<LogToken>): LogToken? {
    return when (this) {
      LogFileFormat.PIPE_SEPARATED -> if (tokens.size > 2) tokens[1] else null
      LogFileFormat.YOUTRACK -> if (tokens.size > 2) tokens[1] else null
      LogFileFormat.PLAIN -> null
    }
  }

  fun extractCategory(tokens: List<LogToken>): LogToken? {
    return when (this) {
      LogFileFormat.PIPE_SEPARATED -> if (tokens.size > 3) tokens[2] else null
      LogFileFormat.YOUTRACK -> if (tokens.size > 3) tokens[2] else null
      LogFileFormat.PLAIN -> null
    }
  }

  fun extractMessage(tokens: List<LogToken>): LogToken {
    return tokens.last { !it.isSeparator }
  }
}

val logFormatKey = Key.create<LogFileFormat>("LogFile.Format")
val youTrackPattern = Pattern.compile("""^\[\d\d:\d\d:\d\d]+.:""")!!
val pipePattern = Pattern.compile("^[0-9:.\\-]+\\s*\\|")!!

fun detectLogFileFormat(editor: Editor): LogFileFormat {
  val existingKey = editor.getUserData(logFormatKey)
  if (existingKey != null)
    return existingKey

  val doc = editor.document.charsSequence
  val firstLines = doc.lineSequence().take(10)
  val pipes = firstLines.sumBy { if (pipePattern.matcher(it).find()) 1 else 0 }
  val youTrackPatterns = firstLines.sumBy { if (youTrackPattern.matcher(it).find()) 1 else 0 }

  val result = when {
    youTrackPatterns > 1 -> LogFileFormat.YOUTRACK
    pipes > 1 -> LogFileFormat.PIPE_SEPARATED
    else -> LogFileFormat.PLAIN
  }

  editor.putUserData(logFormatKey, result)

  return result
}
