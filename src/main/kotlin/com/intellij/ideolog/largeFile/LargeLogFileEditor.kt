package com.intellij.ideolog.largeFile

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.file.update
import com.intellij.ideolog.highlighting.LogHeavyFilterService
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.largeFile.fileType.LargeLogFileType
import com.intellij.ideolog.util.ideologContext
import com.intellij.largeFilesEditor.editor.LargeFileEditor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.util.Disposer

class LargeLogFileEditor(private val delegate: LargeFileEditor) : LargeFileEditor by delegate {
  override fun getName() = IdeologBundle.message("large.log")

  init {
    editor.settings.isUseSoftWraps = false
    this.editor.putUserData(LargeFileEditor.LARGE_FILE_EDITOR_SOFT_WRAP_KEY, false)
    LogHighlightingSettingsStore.getInstance().addSettingsListener(this) {
      resetIdeologStoredData()
    }
    val scheme = EditorColorsManager.getInstance().globalScheme
    val editorHighlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(LargeLogFileType, scheme, project)
    this.trySetHighlighter(editorHighlighter)
  }

  override fun dispose() {
    editor.document.ideologContext.clear()
    Disposer.dispose(delegate)
  }

  private fun resetIdeologStoredData() {
    editor.putUserData(LogHeavyFilterService.markupHighlightedExceptionsKey, null) // don't reset the hyperlink support, that one is safe
    editor.document.ideologContext.clear()

    update(file, project)
  }
}
