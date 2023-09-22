package com.intellij.ideolog.file

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.NonInjectable
import org.jdom.Element

class TotallyNotTextEditorProvider: TextEditorProvider(), DumbAware {
  override fun getEditorTypeId() = "LogFileEditorProvider"

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    return LogFileEditor(project, file, this)
  }

  override fun accept(project: Project, file: VirtualFile) = isTextFile(file) && file.fileType.name == LogFileType.name

  override fun getPolicy(): FileEditorPolicy {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR
  }
}

class LogFileEditorProvider @NonInjectable internal constructor(private val base: TotallyNotTextEditorProvider) : FileEditorProvider by base, DumbAware {
  // used by extension
  @Suppress("unused")
  constructor() : this(TotallyNotTextEditorProvider())
  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) = base.writeState(state,  project, targetElement)
  override fun disposeEditor(editor: FileEditor) = base.disposeEditor(editor)
  override fun readState(sourceElement: Element, project: Project, file: VirtualFile) = base.readState(sourceElement, project, file)
}
