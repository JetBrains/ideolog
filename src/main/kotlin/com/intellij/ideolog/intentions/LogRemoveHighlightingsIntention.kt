package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.highlightingSetUserKey
import com.intellij.ideolog.highlighting.highlightingUserKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class LogRemoveHighlightingsIntention : IntentionAction {
  override fun getText() = "Remove all highlightings"

  override fun getFamilyName() = "Logs"

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false
    val hasColumnHighlight = editor.getUserData(highlightingUserKey) ?: -1 >= 0
    val hasWordHighlight = editor.getUserData(highlightingSetUserKey)?.isNotEmpty() ?: false

    return hasColumnHighlight || hasWordHighlight
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    editor.putUserData(highlightingUserKey, null)
    editor.putUserData(highlightingSetUserKey, null)
    (editor as EditorEx).repaint(0, editor.document.textLength)
    LogFileMapRenderer.getLogFileMapRenderer(editor)?.invalidateHighlighters()
  }

  override fun startInWriteAction() = false
}
