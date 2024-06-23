package com.intellij.ideolog.intentions.base

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.ideolog.util.getSelectedText
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class HideLinesIntention(private val setAccessor: (IdeologDocumentContext) -> HashSet<String>) : IntentionAction {

  private var lastSelection = ""
  val shortSelection: String
    get() = if (lastSelection.length > 25) lastSelection.substring(0, 25) + "..." else lastSelection

  override fun getFamilyName() = IdeologBundle.message("intention.family.name.logs")

  fun getText(editor: Editor): CharSequence? {
    return editor.getSelectedText()
  }

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val text = getText(editor)
    val enabled = text != null
    if (enabled)
      lastSelection = text.toString()
    return enabled

  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    val selection = getText(editor) ?: return

    val set = setAccessor(editor.document.ideologContext)
    set.add(selection.toString())

    FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
  }

  override fun startInWriteAction() = false
}
