package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ResetHiddenItemsIntention : IntentionAction {
  override fun getText(): String = IdeologBundle.message("intention.name.restore.all.hidden.lines")

  override fun getFamilyName(): String = IdeologBundle.message("intention.family.name.logs")

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val context = editor.document.ideologContext
    val hasHiddenItems = context.hiddenItems.isNotEmpty()
    val hasHiddenSubstrings = context.hiddenSubstrings.isNotEmpty()
    val hasWhitelistedSubstrings = context.whitelistedSubstrings.isNotEmpty()
    val hasWhitelistedItems = context.whitelistedItems.isNotEmpty()

    return hasHiddenItems || hasHiddenSubstrings || hasWhitelistedSubstrings || hasWhitelistedItems || context.hideLinesAbove >= 0 || context.hideLinesBelow < Int.MAX_VALUE
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    val context = editor.document.ideologContext
    context.hiddenItems.clear()
    context.hiddenSubstrings.clear()
    context.whitelistedItems.clear()
    context.whitelistedSubstrings.clear()

    context.hideLinesAbove = -1
    context.hideLinesBelow = Int.MAX_VALUE

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
  }

  override fun startInWriteAction(): Boolean = false
}
