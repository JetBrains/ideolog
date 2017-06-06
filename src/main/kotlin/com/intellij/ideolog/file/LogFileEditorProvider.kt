package com.intellij.ideolog.file

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class TotallyNotTextEditorProvider: TextEditorProvider(), DumbAware {
  override fun getEditorTypeId() = "LogFileEditorProvider"

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    return LogFileEditor(project, file, this)
  }

  override fun getTextEditor(editor: Editor): TextEditor {
    return super.getTextEditor(editor)
  }

  override fun getPolicy(): FileEditorPolicy {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR
  }
}

class LogFileEditorProvider(val base: TotallyNotTextEditorProvider = TotallyNotTextEditorProvider()) : FileEditorProvider by base, DumbAware {
  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) = base.writeState(state,  project, targetElement)
  override fun disposeEditor(editor: FileEditor) = base.disposeEditor(editor)
  override fun readState(sourceElement: Element, project: Project, file: VirtualFile) = base.readState(sourceElement, project, file)
}
