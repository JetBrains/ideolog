package com.intellij.ideolog.textmate.highlighting

import com.intellij.ideolog.highlighting.DefaultLogEditorHighlighterProvider
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.textmate.TextMateFileType
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateEditorHighlighterProvider

class TextMateLogFileHighlighterProvider: DefaultLogEditorHighlighterProvider {
  override fun getEditorHighlighter(
    project: Project?,
    fileType: FileType?,
    virtualFile: VirtualFile?,
    colors: EditorColorsScheme,
  ): EditorHighlighter {
    return TextMateEditorHighlighterProvider().getEditorHighlighter(project, TextMateFileType.INSTANCE, virtualFile, colors)
  }
}
