package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.ideolog.highlighting.highlightingUserKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class LogHighlightColumnIntention : IntentionAction {
  override fun getText(): String {
    return IdeologBundle.message("intention.name.highlight.this.column")
  }

  override fun getFamilyName(): String = IdeologBundle.message("intention.family.name.logs")

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val columnCount = LogParsingUtils.getEventColumnCount(editor)
    val currentColumn = LogParsingUtils.getColumnByOffset(editor)

    return currentColumn >= 0 && currentColumn != columnCount - 1
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    editor ?: return
    val column = LogParsingUtils.getColumnByOffset(editor)
    val currentKey = editor.getUserData(highlightingUserKey) ?: -1
    editor.putUserData(highlightingUserKey, if (currentKey == column) -1 else column)
    (editor as EditorEx).repaint(0, editor.document.textLength)
  }

  override fun startInWriteAction(): Boolean = false
}
