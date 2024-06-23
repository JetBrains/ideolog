package com.intellij.ideolog.terminal

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.terminal.highlighting.fileReadCommands
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Document

class IdeologTerminalDocumentContext(document: Document) : IdeologDocumentContext(document, cache = null) {
  companion object {
    const val NUMBER_FIRST_LINES = 100
  }

  private val formatByOffsetMap = hashMapOf<Int, LogFileFormat>()

  override val numberFirstLines: Int
    get() = NUMBER_FIRST_LINES

  override fun clear() {
    super.clear()
    formatByOffsetMap.clear()
  }

  override fun detectLogFileFormat(startOffset: Int): LogFileFormat {
    val currentFormat = formatByOffsetMap[startOffset]
    if (currentFormat != null) return currentFormat
    return super.detectLogFileFormat(startOffset).also {
      formatByOffsetMap[startOffset] = it
    }
  }

  override fun getLogFileFormat(checkedLines: Sequence<String>, regexMatchers: List<RegexLogParser>): LogFileFormat {
    val startTime = System.nanoTime()
    checkedLines.forEach { line ->
      regexMatchers.forEach { regexMatcher ->
        if (System.nanoTime() - startTime < INTERRUPT_AFTER_NS && regexMatcher.regex.matcher(line).find()) {
          return LogFileFormat(regexMatcher)
        }
      }
    }
    return LogFileFormat(null)
  }

  override fun isLineEventStart(format: LogFileFormat, line: CharSequence): Boolean {
    return super.isLineEventStart(format, line) ||
           fileReadCommands.any { baseCommand -> line.startsWith(baseCommand) } && line.endsWith(".log", ignoreCase = true)
  }
}
