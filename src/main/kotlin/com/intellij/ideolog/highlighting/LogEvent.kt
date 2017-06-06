package com.intellij.ideolog.highlighting

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.TrigramBuilder
import com.intellij.psi.impl.cache.impl.id.IdIndexEntry
import gnu.trove.THashSet
import java.util.*

class LogEvent (val rawText: CharSequence, val startOffset: Int, fileType: LogFileFormat) {
    val endOffset = startOffset + rawText.length

    val date : String
    val rawLevel : String
    val category : String
    val message : String
    val fullMessage: String

    val level: String

    val levelIdEntry : IdIndexEntry
    val categoryIdEntry : IdIndexEntry

    val messageTrigrams: THashSet<Int> = THashSet()


    init {
        val tokens: MutableList<LogToken> = ArrayList()
        fileType.tokenize(rawText, tokens)
        val tokensFiltered = tokens.filter { !it.isSeparator }

        date = fileType.extractDate(tokensFiltered)?.takeFrom(rawText)?.trim()?.toString() ?: ""
        rawLevel = fileType.extractSeverity(tokensFiltered)?.takeFrom(rawText)?.trim()?.toString() ?: ""
        category = fileType.extractCategory(tokensFiltered)?.takeFrom(rawText)?.trim().toString()
        fullMessage = fileType.extractMessage(tokensFiltered).takeFrom(rawText).toString().trim()

        level = when (rawLevel.toUpperCase()) {
            "E" -> "ERROR"
            "W" -> "WARN"
            "I" -> "INFO"
            "V" -> "VERBOSE"
            "D" -> "DEBUG"
            "T" -> "TRACE"
            else -> rawLevel.toUpperCase()
        }

        message = fullMessage.split('\n').first().trim()



        levelIdEntry = IdIndexEntry(level, false)
        categoryIdEntry = IdIndexEntry(category, false)

    }

    fun prepareTrigrams() {
        parseTrigrams("\"$message\"", messageTrigrams)
    }


    private fun parseTrigrams(text: String, res: THashSet<Int>) {
        TrigramBuilder.processTrigrams(text, object : TrigramBuilder.TrigramProcessor() {
            override fun execute(value: Int): Boolean {
                res.add(value)
                return true
            }
        })
    }



    companion object {
        fun fromEditor(e: Editor, offset: Int = e.caretModel.offset) : LogEvent {
            val (rawMessage, startOffset) = LogParsingUtils.getEvent(e, offset)
            return LogEvent(rawMessage, startOffset, detectLogFileFormat(e))
        }
    }

    override fun toString(): String {
        return "LogEvent(date=$date, level=$level, category=$category, message=$message)"
    }
}
