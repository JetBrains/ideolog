package com.intellij.ideolog.highlighting

import com.intellij.ideolog.fileType.LogLanguage
import com.intellij.ideolog.highlighting.DefaultLogEditorHighlighterProvider.Companion.DEFAULT_LOG_EDITOR_HIGHLIGHTER_PROVIDER_EP_NAME
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

val LOG_TOKEN_SEPARATOR = IElementType("LOG_TOKEN_SEPARATOR", LogLanguage)

internal val highlightingUserKey = Key.create<Int>("JetLog.HighlightColumn")
internal val highlightingSetUserKey = Key.create<HashSet<String>>("JetLog.HighlightSet")
val highlightTimeKey = Key.create<Boolean>("JetLog.HighlightTime")

const val CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT = FileUtilRt.MEGABYTE / 2

class LogTokenElementType(column: Int) : IElementType("LOG_TOKEN_VALUE_$column", LogLanguage, false)
class LogFileEditorHighlighterProvider : EditorHighlighterProvider {

  override fun getEditorHighlighter(
    project: Project?,
    fileType: FileType,
    virtualFile: VirtualFile?,
    colors: EditorColorsScheme,
  ): EditorHighlighter {
    return LogEditorHighlighter(virtualFile, colors)
  }
}

open class LogEditorHighlighter(
  private val virtualFile: VirtualFile?,
  colors: EditorColorsScheme,
) : EditorHighlighter {
  private var myColors: EditorColorsScheme = colors
  private var myText: CharSequence = ""
  protected var myEditor: HighlighterClient? = null
  private var defaultLogEditorHighlighter: EditorHighlighter = EmptyEditorHighlighter(TextAttributes())

  constructor(colors: EditorColorsScheme) : this(
    virtualFile = null,
    colors = colors
  )

  override fun createIterator(startOffset: Int): HighlighterIterator {
    val defaultHighlighterIterator = doDefaultHighlighterAction { createIterator(startOffset) }
    return defaultHighlighterIterator ?: LogHighlightingIterator(startOffset, myEditor as Editor, { myText }, { myColors })
  }

  override fun setText(text: CharSequence) {
    myText = text
    doDefaultHighlighterAction { setText(text) }
  }

  override fun setEditor(editor: HighlighterClient) {
    myEditor = editor
    DEFAULT_LOG_EDITOR_HIGHLIGHTER_PROVIDER_EP_NAME.extensionList.map { provider ->
      provider.getEditorHighlighter(editor.project, null, virtualFile, myColors)
    }.firstOrNull()?.let { highlighter ->
      if (virtualFile != null && virtualFile.length <= CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT) {
        defaultLogEditorHighlighter = highlighter
      }
    }
  }

  override fun setColorScheme(scheme: EditorColorsScheme) {
    myColors = scheme
  }

  override fun documentChanged(event: DocumentEvent) {
    setText(event.document.charsSequence)
    myEditor?.repaint(0, myText.length)
  }

  private fun <T> doDefaultHighlighterAction(action: EditorHighlighter.() -> T): T? {
    if (myEditor == null || !ApplicationManager.getApplication().isDispatchThread) {
      return defaultLogEditorHighlighter.action()
    }
    val logFileFormat = detectLogFileFormat(myEditor as Editor)
    if (logFileFormat.myRegexLogParser == null) {
      return defaultLogEditorHighlighter.action()
    }
    return null
  }
}
