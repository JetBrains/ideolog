package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.foldings.hiddenItemsKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ResetHiddenItemsIntention : IntentionAction {
  override fun getText() = "Restore all hidden lines"

  override fun getFamilyName() = "Logs"

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val hasHiddenItems = editor.document.getUserData(hiddenItemsKey)?.isNotEmpty() ?: false

    return hasHiddenItems
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    editor.document.putUserData(hiddenItemsKey, null)
    LogHideThisIntention.lastLaunchedTask?.myCancel = true
    ProgressManager.getInstance().run(FoldingCalculatorTask(project, editor, file?.name ?: "?").apply { LogHideThisIntention.lastLaunchedTask = this })
  }

  override fun startInWriteAction() = false
}
