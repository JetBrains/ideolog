package com.intellij.ideolog.file

import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.openapi.diagnostic.Logger
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

  init {
    LogFileMapRenderer.GetOrCreateLogFileMapRenderer(this)
    editor.settings.isUseSoftWraps = false
    (editor as EditorEx).isViewer = true
    editor.putUserData(Key.create("forced.soft.wraps"), java.lang.Boolean.FALSE)
  }

  override fun setState(state: FileEditorState) {
    if(state !is TextEditorState)
      return
    super.setState(state)
    editor.settings.isUseSoftWraps = false
  }

  override fun getState(level: FileEditorStateLevel): FileEditorState {
    return super.getState(level)
  }
}
