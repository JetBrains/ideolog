package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class HideLinesAboveBelowIntentionBase(val setter: (IdeologDocumentContext, Int) -> Unit, private val directionText: String) : IntentionAction {
  override fun getText(): String {
    return IdeologBundle.message("intention.name.hide.lines", directionText)
  }

  override fun getFamilyName(): String {
    return IdeologBundle.message("intention.family.name.logs")
  }

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile?): Boolean {
    return psiFile?.fileType == LogFileType
  }

  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile?) {
    setter(editor.document.ideologContext, editor.caretModel.logicalPosition.line)

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, psiFile)
  }

  override fun startInWriteAction(): Boolean {
    return false
  }
}

class HideLinesAboveIntention: HideLinesAboveBelowIntentionBase({ context, line -> context.hideLinesAbove = line }, "above")
class HideLinesBelowIntention: HideLinesAboveBelowIntentionBase({ context, line -> context.hideLinesBelow = line }, "below")
