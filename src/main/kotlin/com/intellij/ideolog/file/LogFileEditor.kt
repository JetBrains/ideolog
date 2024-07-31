package com.intellij.ideolog.file

import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.LogHeavyFilterService
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

class LogFileEditor(project: Project, file: VirtualFile, provider: TextEditorProvider) : PsiAwareTextEditorImpl(project, file, provider) {
  val log = Logger.getInstance(LogFileEditor::class.java)
  private val isReadOnly: Boolean

  init {
    LogFileMapRenderer.getOrCreateLogFileMapRenderer(this)
    val sizeThreshold = LogHighlightingSettingsStore.getInstance().myState.readonlySizeThreshold.toInt()
    isReadOnly = file.length > sizeThreshold * 1024
    LogHighlightingSettingsStore.getInstance().addSettingsListener(this) {
      resetIdeologStoredData()
    }
    (editor.markupModel as? EditorMarkupModel)?.setTrafficLightIconVisible(false)
    if (isReadOnly) {
      editor.settings.isUseSoftWraps = false
      editor.putUserData(Key.create("forced.soft.wraps"), java.lang.Boolean.FALSE)
    } else {
      editor.document.addDocumentListener(object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          if (event.oldLength != 0 || event.newLength - event.oldLength > 1 || event.offset != event.document.textLength - event.newLength) {
              resetIdeologStoredData()
          }
        }
      })
    }
    editor.isViewer = isReadOnly
  }

  private fun resetIdeologStoredData() {
    editor.putUserData(LogHeavyFilterService.markupHighlightedExceptionsKey, null) // don't reset the hyperlink support, that one is safe
    editor.document.ideologContext.clear()
    LogFileMapRenderer.LogFileMapRendererKey.get(editor)?.detachFromEditor()
    LogFileMapRenderer.getOrCreateLogFileMapRenderer(this)

    LogFileFormatNotificationProvider.update(file, project)
  }

  override fun setState(state: FileEditorState) {
    if(state !is TextEditorState)
      return
    super.setState(state)
    if (isReadOnly)
      editor.settings.isUseSoftWraps = false
  }
}
