package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.containers.isNullOrEmpty

class ResetHiddenItemsIntention : IntentionAction {
  override fun getText() = "Restore all hidden lines"

  override fun getFamilyName() = "Logs"

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val hasHiddenItems = !editor.document.getUserData(hiddenItemsKey).isNullOrEmpty()
    val hasHiddenSubstrings = !editor.document.getUserData(hiddenSubstringsKey).isNullOrEmpty()
    val hasWhitelistedSubstrings = !editor.document.getUserData(whitelistedSubstringsKey).isNullOrEmpty()
    val hasWhitelistedItems = !editor.document.getUserData(whitelistedItemsKey).isNullOrEmpty()

    return hasHiddenItems || hasHiddenSubstrings || hasWhitelistedSubstrings || hasWhitelistedItems || editor.document.getUserData(hideLinesAboveKey) != null || editor.getUserData(hideLinesBelowKey) != null
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    editor.document.putUserData(hiddenItemsKey, null)
    editor.document.putUserData(hiddenSubstringsKey, null)
    editor.document.putUserData(whitelistedSubstringsKey, null)
    editor.document.putUserData(whitelistedItemsKey, null)
    editor.document.putUserData(hideLinesBelowKey, null)
    editor.document.putUserData(hideLinesAboveKey, null)

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
  }

  override fun startInWriteAction() = false
}
