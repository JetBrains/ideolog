package com.intellij.ideolog.terminal.highlighting

import com.intellij.ideolog.util.detectIdeologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.jetbrains.plugins.terminal.block.output.highlighting.TerminalCommandBlockHighlighter
import org.jetbrains.plugins.terminal.block.output.CommandBlock
import java.util.*

private val fileReadCommands = listOf("cat", "head", "tail")

class TerminalCommandBlockHighlighter(
  colorsScheme: EditorColorsScheme
) : TerminalCommandBlockHighlighter {
  private val highlightingInfos = TreeSet<HighlightingInfo>()
  private val highlighter = TerminalLogEditorHighlighter(highlightingInfos, colorsScheme)
  private lateinit var editor: HighlighterClient

  override fun shouldHighlight(startOffset: Int): Boolean {
    if (!::editor.isInitialized) return false
    val document = editor.document
    val lineNumber = document.getLineNumber(startOffset)
    val dummyHighlightingInfo = HighlightingInfo(startOffset)
    synchronized(highlightingInfos) {
      val lowerBoundInfo = highlightingInfos.floor(dummyHighlightingInfo) ?: return false

      return if (lowerBoundInfo.commandStartOffset == startOffset)
        false
      else {
        val followingInfo = highlightingInfos.higher(dummyHighlightingInfo)
                            ?: return lowerBoundInfo.shouldHighlight // startOffset from the last command
        if (document.lineCount > lineNumber + 1 && followingInfo.commandStartOffset == document.getLineStartOffset(lineNumber + 1)) false
        else lowerBoundInfo.shouldHighlight
      }
    }
  }

  override fun applyHighlightingInfoToBlock(block: CommandBlock) {
    val command = block.command
    if (command != null) {
      synchronized(highlightingInfos) {
        highlightingInfos.add(
          HighlightingInfo(
            block.commandStartOffset,
            shouldHighlightCommandBlock(command)
          )
        )
      }
    }
  }

  override fun documentChanged(event: DocumentEvent) {
    if (event.document.textLength == 0) {
      synchronized(highlightingInfos) {
        highlightingInfos.clear()
      }
      return
    }
    val isRemovedNonEmptyPrompt = event.oldLength > event.newLength && !event.oldFragment.isNotBlank()
    if (isRemovedNonEmptyPrompt) {
      synchronized(highlightingInfos) {
        highlightingInfos.forEach { info ->
          if (event.offset < info.commandStartOffset) {
            info.commandStartOffset -= event.oldLength - event.newLength
          }
        }
        highlightingInfos.removeIf { info -> info.commandStartOffset < 0 }
      }
      if (::editor.isInitialized) {
        (editor as? Editor)?.run {
          detectIdeologContext(this).clear()
        }
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

  private fun shouldHighlightCommandBlock(command: CharSequence): Boolean {
    return fileReadCommands.any { baseCommand -> command.startsWith(baseCommand) } && command.endsWith(".log", ignoreCase = true)
  }

  data class HighlightingInfo(
    var commandStartOffset: Int,
    var shouldHighlight: Boolean = false,
  ) : Comparable<HighlightingInfo> {
    override fun compareTo(other: HighlightingInfo): Int {
      return commandStartOffset.compareTo(other.commandStartOffset)
    }
  }
}
