package com.intellij.ideolog.largeFile

import com.intellij.largeFilesEditor.editor.LargeFileEditor
import com.intellij.largeFilesEditor.editor.LargeFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile

private const val PROVIDER_ID = "LargeLogFileEditorProvider"

class LargeLogFileEditorProvider : FileEditorProvider, DumbAware {
  private val largeFileEditorProvider = LargeFileEditorProvider()

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return largeFileEditorProvider.accept(project, file) && Registry.`is`("ideolog.large.file.editor.enabled")
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val largeFileEditor = largeFileEditorProvider.createEditor(project, file) as LargeFileEditor
    return LargeLogFileEditor(largeFileEditor)
  }

  override fun getEditorTypeId() = PROVIDER_ID

  override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}
