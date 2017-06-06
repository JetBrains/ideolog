package com.intellij.ideolog.highlighting

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import java.util.*
import kotlin.concurrent.getOrSet

data class SeparatorScanState(var lastOffset: Int, var goodSeparators: Boolean, var lastLineWithSeparator: Int, var lastLine: Int)
val mySeparatorScanKey = Key.create<SeparatorScanState>("LogSeparatorScanState")

fun Editor.shouldFindTrueEventStart() : Boolean {
  val scanState = document.getUserData(mySeparatorScanKey) ?: SeparatorScanState(0, true, -1, -1)

  if(!scanState.goodSeparators)
    return false

  val fileType = detectLogFileFormat(this)

  val chars = document.charsSequence
  if(scanState.lastOffset < chars.length) {
    var currentOffset = scanState.lastOffset
    while(currentOffset < chars.length) {
      if(currentOffset == 0 || chars[currentOffset - 1] == '\n') {
        scanState.lastLine++
        val contains = fileType.isLineEventStart(chars.subSequence(currentOffset, chars.length))
        if(contains) {
          scanState.lastLineWithSeparator = scanState.lastLine
        } else {
          if(scanState.lastLine - scanState.lastLineWithSeparator > 50000) {
            scanState.goodSeparators = false
            break
          }
        }
      }
      currentOffset++
    }
    scanState.lastOffset = currentOffset
  }
  document.putUserData(mySeparatorScanKey, scanState)
  return scanState.goodSeparators
}

object LogParsingUtils {
  val myTokensTL = ThreadLocal<ArrayList<LogToken>>()
  val myTokens: ArrayList<LogToken>
    get() = myTokensTL.getOrSet { ArrayList() }

  fun getColumnByOffset(editor: Editor): Int {
    return getColumnByOffset(editor.shouldFindTrueEventStart(), detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }

  // -1 if can't determine column
  private fun getColumnByOffset(findTrueEventStart: Boolean, fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int, startIndex: Int = 0, endIndex: Int = data.length): Int {
    val (event, eventOffset) = getEvent(findTrueEventStart, fileType, lineSet, data, offset, startIndex, endIndex)

    val inEventTargetOffset = offset - eventOffset

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    myTokens.asSequence().filter { !it.isSeparator }.forEachIndexed { index, (startOffset, endOffset) ->
      if(inEventTargetOffset in startOffset..endOffset)
        return index
    }

    return -1
  }

  fun getColumnValueByOffset(editor: Editor): CharSequence? {
    return getColumnValueByOffset(editor.shouldFindTrueEventStart(), detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }

  // -1 if can't determine column
  private fun getColumnValueByOffset(findTrueEventStart: Boolean, fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int, startIndex: Int = 0, endIndex: Int = data.length): CharSequence? {
    val (event, eventOffset) = getEvent(findTrueEventStart, fileType, lineSet, data, offset, startIndex, endIndex)

    val inEventTargetOffset = offset - eventOffset

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    myTokens.asSequence().filter { !it.isSeparator }.forEachIndexed { _, logToken ->
      if(logToken.startOffset <= inEventTargetOffset && logToken.endOffset >= inEventTargetOffset)
        return logToken.takeFrom(event)
    }

    return null
  }

  fun getEventColumnCount(editor: Editor): Int {
    return getEventColumnCount(editor.shouldFindTrueEventStart(), detectLogFileFormat(editor), editor.document, editor.document.charsSequence, editor.caretModel.offset)
  }



  private fun getEventColumnCount(findTrueEventStart: Boolean, fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int, startIndex: Int = 0, endIndex: Int = data.length): Int {
    val (event, _) = getEvent(findTrueEventStart, fileType, lineSet, data, offset, startIndex, endIndex)

    myTokens.clear()
    fileType.tokenize(event, myTokens)

    return myTokens.asSequence().filter { !it.isSeparator }.count()
  }

  fun getEvent(editor: Editor, offset : Int = editor.caretModel.offset): Pair<CharSequence, Int> {
    return getEvent(editor.shouldFindTrueEventStart(), detectLogFileFormat(editor), editor.document, editor.document.charsSequence, offset)
  }

  fun getEvent(findTrueEventStart: Boolean, fileType: LogFileFormat, lineSet: Document, data: CharSequence, offset: Int, startIndex: Int = 0, endIndex: Int = data.length): Pair<CharSequence, Int> {
    val eventStartOffset = findEventStartOffset(findTrueEventStart, fileType, data, lineSet, offset, startIndex, endIndex)
    var nextEventStartOffset = eventStartOffset
    do {
      nextEventStartOffset++
    } while(!(nextEventStartOffset >= endIndex || data[nextEventStartOffset - 1] == '\n' && (!findTrueEventStart || fileType.isLineEventStart(data.subSequence(nextEventStartOffset, endIndex)))))

    return data.subSequence(eventStartOffset, Math.min(nextEventStartOffset, endIndex)) to eventStartOffset
  }

  fun findEventStartOffset(findTrueEventStart: Boolean, fileType: LogFileFormat, data: CharSequence, lineSet: Document, offset: Int, startIndex: Int = 0, endIndex: Int = data.length) : Int {
    var lineStartOffset = offset

    var lineIndex = lineSet.getLineNumber(if(lineStartOffset < 0) 0 else lineStartOffset)

    do {
      lineStartOffset = lineSet.getLineStartOffset(lineIndex)

      if(lineIndex == 0 || lineStartOffset <= startIndex || !findTrueEventStart || fileType.isLineEventStart(data.subSequence(lineStartOffset, endIndex)))
        break

      lineIndex--
    } while(true)

    return lineStartOffset
  }
}
