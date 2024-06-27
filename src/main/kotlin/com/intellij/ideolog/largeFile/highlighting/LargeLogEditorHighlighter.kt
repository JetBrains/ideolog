package com.intellij.ideolog.largeFile.highlighting

import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes

class LargeLogEditorHighlighter(colors: EditorColorsScheme) : LogEditorHighlighter(colors) {
  private var myColors: EditorColorsScheme = colors

  override fun createIterator(startOffset: Int): HighlighterIterator {
    if (myEditor == null || !ApplicationManager.getApplication().isDispatchThread)
      return EmptyEditorHighlighter(TextAttributes()).createIterator(startOffset)

    return LargeLogHighlightingIterator(startOffset, myEditor as Editor, { "" }) { myColors }
  }

  override fun documentChanged(event: DocumentEvent) {
  }

  override fun setColorScheme(scheme: EditorColorsScheme) {
    myColors = scheme
  }
}
