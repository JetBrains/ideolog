package com.intellij.ideolog.highlighting

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import java.util.*
import kotlin.concurrent.getOrSet

object LogParsingUtils {
  val myTokensTL = ThreadLocal<ArrayList<LogToken>>()
  val myTokens: ArrayList<LogToken>
    get() = myTokensTL.getOrSet { ArrayList() }

  fun getColumnByOffset(editor: Editor): Int {
    return getColumnByOffset(detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }

  // -1 if can't determine column
  private fun getColumnByOffset(fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int): Int {
    val (event, eventOffset) = getEvent(lineSet, data, offset)

    val inEventTargetOffset = offset - eventOffset

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    myTokens.asSequence().filter { !it.isSeparator }.forEachIndexed { index, (startOffset, endOffset) ->
      if (inEventTargetOffset in startOffset..endOffset)
        return index
    }

    return -1
  }

  fun getColumnValueByOffset(editor: Editor): CharSequence? {
    return getColumnValueByOffset(detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }

  // -1 if can't determine column
  private fun getColumnValueByOffset(fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int): CharSequence? {
    val (event, eventOffset) = getEvent(lineSet, data, offset)

    val inEventTargetOffset = offset - eventOffset

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    myTokens.asSequence().filter { !it.isSeparator }.forEachIndexed { _, logToken ->
      if (logToken.startOffset <= inEventTargetOffset && logToken.endOffset >= inEventTargetOffset)
        return logToken.takeFrom(event)
    }

    return null
  }

  fun getEventColumnCount(editor: Editor): Int {
    return getEventColumnCount(detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }


  private fun getEventColumnCount(fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int): Int {
    val (event, _) = getEvent(lineSet, data, offset)

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    return myTokens.asSequence().filter { !it.isSeparator }.count()
  }

  fun getEvent(editor: Editor, offset: Int = editor.caretModel.offset): Pair<CharSequence, Int> {
    return getEvent(editor.document, editor.document.charsSequence, offset)
  }

  private fun getEvent(lineSet: Document, data: CharSequence, offset: Int): Pair<CharSequence, Int> {
    val line = lineSet.getLineNumber(offset)

    val eventLines = lineSet.ideologContext.getEvent(line)
    val startIndex = lineSet.getLineStartOffset(eventLines.first)
    return data.subSequence(startIndex, lineSet.getLineEndOffset(eventLines.last)) to startIndex
  }
}
