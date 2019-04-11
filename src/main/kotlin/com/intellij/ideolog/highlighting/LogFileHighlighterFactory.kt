package com.intellij.ideolog.highlighting

import com.intellij.ideolog.fileType.LogLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import java.util.*

val LOG_TOKEN_SEPARATOR = IElementType("LOG_TOKEN_SEPARATOR", LogLanguage)

internal val highlightingUserKey = Key.create<Int>("JetLog.HighlightColumn")
internal val highlightingSetUserKey = Key.create<HashSet<String>>("JetLog.HighlightSet")
val highlightTimeKey = Key.create<Boolean>("JetLog.HighlightTime")

class LogTokenElementType(val column: Int) : IElementType("LOG_TOKEN_VALUE_$column", LogLanguage, false)
class LogFileEditorHighlighterProvider : EditorHighlighterProvider {

  override fun getEditorHighlighter(project: Project?, fileType: FileType, virtualFile: VirtualFile?, colors: EditorColorsScheme): EditorHighlighter {
    return LogEditorHighlighter(colors)
  }
}

class LogEditorHighlighter(colors: EditorColorsScheme) : EditorHighlighter {
  private var myColors: EditorColorsScheme = colors
  private var myText: CharSequence = ""
  private var myEditor: HighlighterClient? = null


  override fun createIterator(startOffset: Int): HighlighterIterator {
    if (myEditor == null)
      return EmptyEditorHighlighter(TextAttributes()).apply { setText(myText) }.createIterator(startOffset)

    return LogHighlightingIterator(startOffset, myEditor as Editor, { myText }, { myColors })
  }

  override fun setText(text: CharSequence) {
    myText = text
  }

  override fun setEditor(editor: HighlighterClient) {
    myEditor = editor
  }

  override fun setColorScheme(scheme: EditorColorsScheme) {
    myColors = scheme
  }

  override fun documentChanged(event: DocumentEvent) {
    myText = event.document.charsSequence
    myEditor?.repaint(0, myText.length)
  }
}
