package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class HideLinesAboveBelowIntentionBase(val setter: (IdeologDocumentContext, Int) -> Unit, private val directionText: String) : IntentionAction {
  override fun getText(): String {
    return "Hide lines $directionText"
  }

  override fun getFamilyName(): String {
    return "Logs"
  }

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    return true
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    setter(editor.document.ideologContext, editor.caretModel.logicalPosition.line)

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
  }

  override fun startInWriteAction(): Boolean {
    return false
  }
}

class HideLinesAboveIntention: HideLinesAboveBelowIntentionBase({ context, line -> context.hideLinesAbove = line }, "above")
class HideLinesBelowIntention: HideLinesAboveBelowIntentionBase({ context, line -> context.hideLinesBelow = line }, "below")
