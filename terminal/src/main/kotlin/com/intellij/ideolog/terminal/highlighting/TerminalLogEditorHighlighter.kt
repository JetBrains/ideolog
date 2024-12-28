package com.intellij.ideolog.terminal.highlighting

import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import java.util.*

internal class TerminalLogEditorHighlighter(
  private val highlightingInfos: TreeSet<TerminalCommandBlockHighlighter.HighlightingInfo>,
  colors: EditorColorsScheme
) : LogEditorHighlighter(colors) {
  private var myColors: EditorColorsScheme = colors

  override fun createIterator(startOffset: Int): HighlighterIterator {
    if (myEditor == null || !ApplicationManager.getApplication().isDispatchThread)
      return EmptyEditorHighlighter(TextAttributes()).createIterator(startOffset)

    return TerminalLogHighlightingIterator(highlightingInfos, startOffset, myEditor as Editor, { "" }) { myColors }
  }
}
