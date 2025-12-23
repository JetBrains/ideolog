package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.highlightingSetUserKey
import com.intellij.ideolog.highlighting.highlightingUserKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

internal class LogRemoveHighlightingsIntention : IntentionAction {
  override fun getText() = IdeologBundle.message("intention.name.remove.all.highlightings")

  override fun getFamilyName() = IdeologBundle.message("intention.family.name.logs")

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile?): Boolean {
    if (psiFile?.fileType != LogFileType)
      return false
    val hasColumnHighlight = (editor.getUserData(highlightingUserKey) ?: -1) >= 0
    val hasWordHighlight = editor.getUserData(highlightingSetUserKey)?.isNotEmpty() ?: false

    return hasColumnHighlight || hasWordHighlight
  }

  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile?) {
    editor.putUserData(highlightingUserKey, null)
    editor.putUserData(highlightingSetUserKey, null)
    (editor as EditorEx).repaint(0, editor.document.textLength)
    LogFileMapRenderer.getLogFileMapRenderer(editor)?.invalidateHighlighters()
  }

  override fun startInWriteAction() = false
}
