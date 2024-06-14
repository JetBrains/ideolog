package com.intellij.ideolog.largeFile

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.largeFilesEditor.editor.LargeFileEditor
import com.intellij.largeFilesEditor.editor.LargeFileEditorProvider
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LargeLogFileEditorProvider : FileEditorProvider, DumbAware {
  private val largeFileEditorProvider = LargeFileEditorProvider()

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return largeFileEditorProvider.accept(project, file)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val editorHighlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(LogFileType, scheme, project)

    val largeFileEditor = largeFileEditorProvider.createEditor(project, file) as LargeFileEditor
    largeFileEditor.trySetHighlighter(editorHighlighter)
    return LargeLogFileEditor(largeFileEditor)
  }

  override fun getEditorTypeId() = largeFileEditorProvider.editorTypeId

  override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}
