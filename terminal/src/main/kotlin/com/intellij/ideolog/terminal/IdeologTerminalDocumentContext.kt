package com.intellij.ideolog.terminal

import com.intellij.ideolog.largeFile.getLogFileFormatByFirstMatch
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.terminal.highlighting.fileReadCommands
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Document

class IdeologTerminalDocumentContext(document: Document) : IdeologDocumentContext(document, cache = null) {
  companion object {
    private const val NUMBER_FIRST_LINES = 100
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

  override fun getLogFileFormat(fileLines: Sequence<String>, regexMatchers: List<RegexLogParser>): LogFileFormat {
    return getLogFileFormatByFirstMatch(fileLines, regexMatchers)
  }

  override fun isLineEventStart(atLine: Int): Boolean {
    val line = lineCharSequence(atLine)
    if (isLogFileReadCommand(line)) return true
    val lineStartOffset = document.getLineStartOffset(atLine)
    val format = detectLogFileFormat(lineStartOffset).also {
      formatByOffsetMap[lineStartOffset] = it
    }
    if (format.myRegexLogParser == null && line.isNotEmpty()) return false
    return format.isLineEventStart(lineCharSequence(atLine))
  }

  private fun isLogFileReadCommand(line: CharSequence): Boolean {
    return fileReadCommands.any { baseCommand -> line.startsWith(baseCommand) } && line.endsWith(".log", ignoreCase = true)
  }
}
