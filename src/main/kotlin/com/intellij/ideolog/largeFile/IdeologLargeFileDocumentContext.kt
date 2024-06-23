package com.intellij.ideolog.largeFile

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.text.StringUtil

private const val FORMAT_DETECTION_DELAY_MILLIS: Long = 100

fun getLogFileFormatByFirstMatch(fileLines: Sequence<String>, regexMatchers: List<RegexLogParser>): LogFileFormat {
  fileLines.forEach { line ->
    val bombedCharSequence = StringUtil.newBombedCharSequence(line, FORMAT_DETECTION_DELAY_MILLIS)
    val matchedRegex = matchLogFileFormatRegex(bombedCharSequence, regexMatchers)
    if (matchedRegex != null) {
      return LogFileFormat(matchedRegex)
    }
  }
  return LogFileFormat(null)
}

fun matchLogFileFormatRegex(input: CharSequence, regexMatchers: List<RegexLogParser>): RegexLogParser? {
  regexMatchers.forEach { regexMatcher ->
    try {
      if (regexMatcher.regex.matcher(input).find()) {
        return regexMatcher
      }
    }
    catch (_: ProcessCanceledException) {
    }
  }
  return null
}

class IdeologLargeFileDocumentContext(document: Document) : IdeologDocumentContext(document, cache = null) {
  override fun detectLogFileFormat(startOffset: Int): LogFileFormat {
    if (document.textLength == 0) return LogFileFormat(null)
    return super.detectLogFileFormat(0)
  }

  override fun getLogFileFormat(fileLines: Sequence<String>, regexMatchers: List<RegexLogParser>): LogFileFormat {
    return getLogFileFormatByFirstMatch(fileLines, regexMatchers)
  }
}
