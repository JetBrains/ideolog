package com.intellij.ideolog.largeFile

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.largeFile.fileType.LargeLogFileType
import com.intellij.largeFilesEditor.editor.LargeFileEditor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory

class LargeLogFileEditor(delegate: LargeFileEditor) : LargeFileEditor by delegate {
  override fun getName() = IdeologBundle.message("large.log")

  init {
    editor.settings.isUseSoftWraps = false
    this.editor.putUserData(LargeFileEditor.LARGE_FILE_EDITOR_SOFT_WRAP_KEY, false)
    val scheme = EditorColorsManager.getInstance().globalScheme
    val editorHighlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(LargeLogFileType, scheme, project)
    this.trySetHighlighter(editorHighlighter)
  }
}
