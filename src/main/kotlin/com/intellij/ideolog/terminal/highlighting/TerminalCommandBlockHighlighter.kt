package com.intellij.ideolog.terminal.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.jetbrains.plugins.terminal.exp.CommandBlock
import org.jetbrains.plugins.terminal.exp.TerminalCommandBlockHighlighter
import java.util.*

private val fileReadCommands = listOf("cat", "head", "tail", "less", "more")

class TerminalCommandBlockHighlighter(
  colorsScheme: EditorColorsScheme
): TerminalCommandBlockHighlighter {
  private val highlightingInfos = TreeSet<HighlightingInfo> { first, second -> first.commandStartOffset - second.commandStartOffset }
  private val highlighter = TerminalLogEditorHighlighter(highlightingInfos, colorsScheme)
  private val infoSetLock = Any()
  private lateinit var editor: HighlighterClient

  override fun shouldHighlight(startOffset: Int): Boolean {
    if (!::editor.isInitialized) return false
    val document = editor.document
    val lineNumber = document.getLineNumber(startOffset)
    val dummyHighlightingInfo = HighlightingInfo(startOffset)
    synchronized(infoSetLock) {
      val lowerBoundOutput = highlightingInfos.floor(dummyHighlightingInfo) ?: return false

      return if (lowerBoundOutput.commandStartOffset == startOffset)
        false
      else {
        val nextOutput = highlightingInfos.higher(dummyHighlightingInfo) ?: return lowerBoundOutput.shouldHighlight // startOffset from the last command
        if (document.lineCount > lineNumber + 1 && nextOutput.commandStartOffset == document.getLineStartOffset(lineNumber + 1)) false else lowerBoundOutput.shouldHighlight
      }
    }
  }

  override fun applyHighlightingInfoToBlock(block: CommandBlock) {
    if (block.command != null) {
      synchronized(infoSetLock) {
        highlightingInfos.add(
          HighlightingInfo(
            block.commandStartOffset,
            shouldHighlightCommandOutputInfo(block.command!!)
          )
        )
      }
    }
  }

  override fun documentChanged(event: DocumentEvent) {
    if (event.document.textLength == 0) {
      synchronized(infoSetLock) {
        highlightingInfos.clear()
      }
    }
  }

  override fun createIterator(startOffset: Int): HighlighterIterator {
    return highlighter.createIterator(startOffset)
  }

  override fun setEditor(editor: HighlighterClient) {
    highlighter.setEditor(editor)
    this.editor = editor
  }

  private fun shouldHighlightCommandOutputInfo(command: CharSequence): Boolean {
    return fileReadCommands.any { baseCommand -> command.startsWith(baseCommand) } && command.endsWith(".log", ignoreCase = true)
  }

  data class HighlightingInfo(
    val commandStartOffset: Int,
    var shouldHighlight: Boolean = false,
  )
}
