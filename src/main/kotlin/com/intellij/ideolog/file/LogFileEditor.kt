package com.intellij.ideolog.file

import com.intellij.ideolog.foldings.hideLinesAboveKey
import com.intellij.ideolog.foldings.hideLinesBelowKey
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.logSeparatorScanKey
import com.intellij.ideolog.highlighting.markupHighlightedExceptionsKey
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.logFormatKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
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
    LogFileMapRenderer.GetOrCreateLogFileMapRenderer(this)
    val sizeThreshold = LogHighlightingSettingsStore.getInstance().myState.readonlySizeThreshold.toInt()
    isReadOnly = file.length > sizeThreshold * 1024
    if (isReadOnly) {
      editor.settings.isUseSoftWraps = false
      editor.putUserData(Key.create("forced.soft.wraps"), java.lang.Boolean.FALSE)
    } else {
      fun resetIdeologStoredData() {
        editor.putUserData(logSeparatorScanKey, null)
        editor.putUserData(markupHighlightedExceptionsKey, null)
        editor.putUserData(hideLinesAboveKey, null)
        editor.putUserData(hideLinesBelowKey, null)
        editor.putUserData(logFormatKey, null)
        LogFileMapRenderer.LogFileMapRendererKey.get(editor)?.detachFromEditor()
        LogFileMapRenderer.GetOrCreateLogFileMapRenderer(this)
      }

      editor.document.addDocumentListener(object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          if (event.oldLength != 0 || event.offset != event.document.textLength - event.newLength) {
              resetIdeologStoredData()
          }
        }
      })
    }
    (editor as EditorEx).isViewer = isReadOnly
  }

  override fun setState(state: FileEditorState) {
    if(state !is TextEditorState)
      return
    super.setState(state)
    if (isReadOnly)
      editor.settings.isUseSoftWraps = false
  }

  override fun getState(level: FileEditorStateLevel): FileEditorState {
    return super.getState(level)
  }
}
