package com.intellij.ideolog.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface DefaultLogEditorHighlighterProvider {
  companion object {
    internal val DEFAULT_LOG_EDITOR_HIGHLIGHTER_PROVIDER_EP_NAME: ExtensionPointName<DefaultLogEditorHighlighterProvider> =
      ExtensionPointName.create(
        "com.intellij.ideolog.defaultLogEditorHighlighterProvider"
      )
  }

  fun getEditorHighlighter(project: Project?, fileType: FileType?, virtualFile: VirtualFile?, colors: EditorColorsScheme): EditorHighlighter
}
