package com.intellij.ideolog.largeFile

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Document

class IdeologLargeFileDocumentContext(document: Document) : IdeologDocumentContext(document, cache = null) {
  override fun detectLogFileFormat(startOffset: Int): LogFileFormat {
    if (document.textLength == 0) return LogFileFormat(null)
    return super.detectLogFileFormat(0)
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
}
