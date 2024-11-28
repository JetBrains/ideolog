package com.intellij.ideolog.lex

import com.intellij.ideolog.highlighting.LOG_TOKEN_SEPARATOR
import com.intellij.ideolog.highlighting.LogTokenElementType
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class LogFileLexer(private val tokenCache: MutableList<IElementType>, private var findEventStart: Boolean, var fileType: LogFileFormat) : LexerBase() {
  private var myBuffer: CharSequence = ""
  private var myBufferEnd: Int = -1
  private var myBufferStart: Int = -1
  private var myColumn: Int = 0

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

    if (newTokenPosition >= myBufferEnd)
      return

    var seenNewLine = false
    while (true) {
      if (newTokenPosition >= myBufferEnd || !seenNewLine && myBuffer[newTokenPosition] == '|')
        break
      if (myBuffer[newTokenPosition] == '\n') {
        seenNewLine = true
        if (!findEventStart || fileType.isLineEventStart(myBuffer.subSequence(newTokenPosition + 1, myBufferEnd))) break
      }
      newTokenPosition++
    }

    if (newTokenPosition == newTokenStart)
      newTokenPosition++

    currentTokenStart = newTokenStart
    currentTokenEnd = newTokenPosition
    if (newTokenStart == newTokenPosition - 1 && myBuffer[newTokenStart].let { it == '|' || it == '\n' }) {
      currentTokenType = LOG_TOKEN_SEPARATOR
      if (myBuffer[newTokenStart] == '\n')
        myColumn = 0
      else
        myColumn++
    } else
      currentTokenType = getOrMakeElementToken()
  }

  companion object {
    fun lexRegex(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean, parser: RegexLogParser) {
      val endOfLineIndex = event.indexOf('\n')
      val eventIsMultiline = endOfLineIndex != -1
      val dataToMatch = if (parser.otherParsingSettings.regexMatchFullEvent || !eventIsMultiline) event else event.subSequence(0, endOfLineIndex)

      val matcher = parser.regex.matcher(dataToMatch)
      if (!matcher.find() || matcher.groupCount() == 0) {
        output.add(LogToken(0, event.length, false))
        return
      }

      var lastGroupEnd = 0
      for(i in 1 .. matcher.groupCount()) {
        val start = matcher.start(i)
        if (start < lastGroupEnd) continue

        val end = matcher.end(i)

        if(end < 0)
          continue

        if(start > lastGroupEnd) {
          if(!onlyValues)
            output.add(LogToken(lastGroupEnd, start, true))
        }

        output.add(LogToken(start, end, false))

        lastGroupEnd = end
      }

      if (eventIsMultiline && !parser.otherParsingSettings.regexMatchFullEvent) {
        val lastToken = output.removeAt(output.lastIndex)
        output.add(LogToken(lastToken.startOffset, event.length, false))
      } else if(lastGroupEnd < matcher.end() && !onlyValues) {
        output.add(LogToken(lastGroupEnd, matcher.end(), true))
      }
    }

    fun lexPlainLog(event: CharSequence, output: MutableList<LogToken>, onlyValues: Boolean) {
      val firstSpace = event.indexOf(' ')
      if (firstSpace <= 0) {
        output.add(LogToken(0, event.length, false))
        return
      }
      output.add(LogToken(0, firstSpace, false))
      if (!onlyValues)
        output.add(LogToken(firstSpace, firstSpace + 1, true))
      output.add(LogToken(firstSpace + 1, event.length, false))
    }
  }

  private fun getOrMakeElementToken(): IElementType {
    while (myColumn >= tokenCache.size) {
      tokenCache.add(LogTokenElementType(tokenCache.size))
    }
    return tokenCache[myColumn]
  }

  override fun getBufferSequence(): CharSequence = myBuffer

  override fun getBufferEnd(): Int = myBufferEnd
}
