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
import com.intellij.openapi.vfs.limits.FileSizeLimit

private const val PROVIDER_ID = "LargeLogFileEditorProvider"

class LargeLogFileEditorProvider : FileEditorProvider, DumbAware {
  private val largeFileEditorProvider = LargeFileEditorProvider()

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return Registry.`is`("ideolog.large.file.editor.enabled") &&
           largeFileEditorProvider.accept(project, file) &&
           file.length > FileSizeLimit.getContentLoadLimit("log")
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val largeFileEditor = largeFileEditorProvider.createEditor(project, file) as LargeFileEditor
    return LargeLogFileEditor(largeFileEditor)
  }

  override fun getEditorTypeId() = PROVIDER_ID

  override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}
