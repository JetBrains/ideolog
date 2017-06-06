package com.intellij.ideolog.lex

import com.intellij.ideolog.highlighting.LOG_TOKEN_SEPARATOR
import com.intellij.ideolog.highlighting.LogTokenElementType
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class LogFileLexer(val tokenCache: MutableList<IElementType>, var findEventStart: Boolean, var fileType: LogFileFormat) : LexerBase() {
  private var myBuffer: CharSequence = ""
  private var myBufferEnd: Int = -1
  private var myBufferStart: Int = -1
  private var myColumn : Int = 0

  private var currentTokenStart: Int = -1
  private var currentTokenEnd: Int = -1
  private var currentTokenType: IElementType? = null

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    myBuffer = buffer
    myBufferStart = startOffset
    myBufferEnd = endOffset
    myColumn = initialState

    currentTokenEnd = -1
    currentTokenStart = -1
    currentTokenType = null

    advance()
  }

  override fun getState(): Int = myColumn

  override fun getTokenType(): IElementType? = currentTokenType

  override fun getTokenStart(): Int = currentTokenStart

  override fun getTokenEnd(): Int = currentTokenEnd

  override fun advance() {
    val newTokenStart = if (currentTokenEnd < 0) myBufferStart else currentTokenEnd
    currentTokenType = null
    var newTokenPosition = newTokenStart

    if(newTokenPosition >= myBufferEnd)
      return

    var seenNewLine = false
    while (true)
    {
      if(newTokenPosition >= myBufferEnd || !seenNewLine && myBuffer[newTokenPosition] == '|')
        break
      if (myBuffer[newTokenPosition] == '\n') {
        seenNewLine = true
        if (!findEventStart || fileType.isLineEventStart(myBuffer.subSequence(newTokenPosition + 1, myBufferEnd))) break
      }
      newTokenPosition++
    }

    if(newTokenPosition == newTokenStart)
      newTokenPosition++

    currentTokenStart = newTokenStart
    currentTokenEnd = newTokenPosition
    if(newTokenStart == newTokenPosition - 1 && myBuffer[newTokenStart].let { it == '|' || it == '\n'}) {
      currentTokenType = LOG_TOKEN_SEPARATOR
      if(myBuffer[newTokenStart] == '\n')
        myColumn = 0
      else
        myColumn++
    }
    else
      currentTokenType = getOrMakeElementToken()
  }

  companion object {
    fun lexPipeLine(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean) {
      var offset = 0
      val firstThousand = event.subSequence(0, Math.min(event.length, 100))
      firstThousand.splitToSequence('|').forEach {
        output.add(LogToken(offset, offset + it.length, false))
        offset += it.length
        if(!onlyValues)
          output.add(LogToken(offset, offset + 1, true))
        offset++
      }
      if (!onlyValues && output.size > 0)
        output.removeAt(output.size - 1)
      if(output.size > 0 && event.length >= 100)
        output.last().endOffset = event.length
    }

    fun lexYoutrackLine(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean) {
      val closingSq = event.indexOf(']')
      if (closingSq == -1) {
        output.add(LogToken(0, event.length, false))
        return
      }
      val colon = event.indexOf(':', closingSq)
      if (colon == -1) {
        output.add(LogToken(0, event.length, false))
        return
      }

      if(!onlyValues)
        output.add(LogToken(0, 1, true))

      output.add(LogToken(1, closingSq, false))
      if(!onlyValues)
        output.add(LogToken(closingSq, closingSq + 1, true))

      output.add(LogToken(closingSq + 1, colon, false))

      val catStart = event.indexOf('[', colon)
      val catEnd = event.indexOf(']', colon)

      if(catStart == -1 || catEnd == -1) {
        output.add(LogToken(colon + 1, event.length, false))
        return
      }

      if(!onlyValues)
        output.add(LogToken(colon, catStart + 1, true))

      output.add(LogToken(catStart + 1, catEnd, false))
      if(!onlyValues)
        output.add(LogToken(catEnd, catEnd + 1, true))

      output.add(LogToken(catEnd + 1, event.length, false))
    }

    fun lexPlainLog(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean) {
      val firstSpace = event.indexOf(' ')
      if(firstSpace <= 0) {
        output.add(LogToken(0, event.length, false))
        return
      }
      output.add(LogToken(0, firstSpace, false))
      if(!onlyValues)
        output.add(LogToken(firstSpace, firstSpace + 1, true))
      output.add(LogToken(firstSpace + 1, event.length, false))
    }
  }

  fun getOrMakeElementToken(): IElementType {
    while (myColumn >= tokenCache.size) {
      tokenCache.add(LogTokenElementType(tokenCache.size))
    }
    return tokenCache[myColumn]
  }

  override fun getBufferSequence(): CharSequence = myBuffer

  override fun getBufferEnd(): Int = myBufferEnd
}
