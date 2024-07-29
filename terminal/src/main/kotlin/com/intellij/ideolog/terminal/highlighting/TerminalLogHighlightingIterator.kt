package com.intellij.ideolog.terminal.highlighting

import com.intellij.ideolog.highlighting.LogHighlightingIterator
import com.intellij.ideolog.highlighting.settings.LogHighlightingPattern
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import java.util.*

class TerminalLogHighlightingIterator(
  private val highlightingInfos: TreeSet<TerminalCommandBlockHighlighter.HighlightingInfo>,
  startOffset: Int,
  myEditor: Editor,
  textGetter: () -> CharSequence,
  colorGetter: () -> EditorColorsScheme
) : LogHighlightingIterator(startOffset, myEditor, textGetter, colorGetter) {
  override fun detectLogFileFormatByOffset(editor: Editor, offset: Int): LogFileFormat {
    val dummyOutputInfo = TerminalCommandBlockHighlighter.HighlightingInfo(offset)
    val lowerBoundInfo = highlightingInfos?.floor(dummyOutputInfo) // first call happens before highlightingInfos initialization
    val realOffset = lowerBoundInfo?.commandStartOffset ?: offset
    return detectLogFileFormat(editor, realOffset)
  }

  override fun tryHighlightStacktrace(event: CharSequence, eventOffset: Int) {
  }

  override fun acceptHighlighter(logHighlightingPattern: LogHighlightingPattern, fileFormat: LogFileFormat, captureGroup: Int): Boolean {
    return fileFormat.myRegexLogParser != null && super.acceptHighlighter(logHighlightingPattern, fileFormat, captureGroup)
  }
}
