package com.intellij.ideolog.highlighting

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.TrigramBuilder
import gnu.trove.THashSet
import java.util.*

class LogEvent(val rawText: CharSequence, val startOffset: Int, fileType: LogFileFormat) {
  val endOffset = startOffset + rawText.length

  val date: String
  private val rawLevel: String
  val category: String
  val message: String
  private val fullMessage: String

  val level: String

  val messageTrigrams: THashSet<Int> = THashSet()

  init {
    val tokens: MutableList<LogToken> = ArrayList()
    fileType.tokenize(rawText, tokens)
    val tokensFiltered = tokens.filter { !it.isSeparator }

    date = fileType.extractDate(tokensFiltered)?.takeFrom(rawText)?.trim()?.toString() ?: ""
    rawLevel = fileType.extractSeverity(tokensFiltered)?.takeFrom(rawText)?.trim()?.toString() ?: ""
    category = fileType.extractCategory(tokensFiltered)?.takeFrom(rawText)?.trim()?.toString() ?: ""
    fullMessage = fileType.extractMessage(tokensFiltered).takeFrom(rawText).toString().trim()

    level = when (rawLevel.uppercase(Locale.getDefault())) {
      "E" -> "ERROR"
      "W" -> "WARN"
      "I" -> "INFO"
      "V" -> "VERBOSE"
      "D" -> "DEBUG"
      "T" -> "TRACE"
      "SEVERE" -> "ERROR"
      else -> rawLevel.uppercase(Locale.getDefault())
    }

    message = fullMessage.split('\n').first().trim()
  }

  fun prepareTrigrams() {
    parseTrigrams("\"$message\"", messageTrigrams)
  }


  private fun parseTrigrams(text: String, res: THashSet<Int>) {
    TrigramBuilder.processTrigrams(text, object : TrigramBuilder.TrigramProcessor() {
      override fun consumeTrigramsCount(count: Int): Boolean {
        res.ensureCapacity(count)
        return true
      }

      override fun test(value: Int): Boolean {
        res.add(value)
        return true
      }
    })
  }


  companion object {
    fun fromEditor(e: Editor, offset: Int = e.caretModel.offset): LogEvent {
      val (rawMessage, startOffset) = LogParsingUtils.getEvent(e, offset)
      return LogEvent(rawMessage, startOffset, detectLogFileFormat(e))
    }
  }

  override fun toString(): String {
    return "LogEvent(date=$date, level=$level, category=$category, message=$message)"
  }
}
