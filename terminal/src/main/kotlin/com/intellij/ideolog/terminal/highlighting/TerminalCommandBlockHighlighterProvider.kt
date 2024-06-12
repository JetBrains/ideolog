package com.intellij.ideolog.terminal.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import org.jetbrains.plugins.terminal.block.output.highlighting.TerminalCommandBlockHighlighter
import org.jetbrains.plugins.terminal.block.output.highlighting.TerminalCommandBlockHighlighterProvider

class TerminalCommandBlockHighlighterProvider : TerminalCommandBlockHighlighterProvider {
  override fun getHighlighter(colorsScheme: EditorColorsScheme): TerminalCommandBlockHighlighter {
    return TerminalCommandBlockHighlighter(colorsScheme)
  }
}
