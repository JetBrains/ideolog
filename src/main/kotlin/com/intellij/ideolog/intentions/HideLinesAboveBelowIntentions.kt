package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.foldings.hideLinesAboveKey
import com.intellij.ideolog.foldings.hideLinesBelowKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

/**
 * @author Nikolay.Kuznetsov
 */
abstract class HideLinesAboveBelowIntentionBase(val key: Key<Int>, val directionText: String) : IntentionAction {
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
    editor.document.putUserData(key, editor.caretModel.logicalPosition.line)

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
  }

  override fun startInWriteAction(): Boolean {
    return false
  }
}

class HideLinesAboveIntention: HideLinesAboveBelowIntentionBase(hideLinesAboveKey, "above")
class HideLinesBelowIntention: HideLinesAboveBelowIntentionBase(hideLinesBelowKey, "below")
