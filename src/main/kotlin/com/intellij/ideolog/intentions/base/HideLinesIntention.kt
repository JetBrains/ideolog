package com.intellij.ideolog.intentions.base

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

abstract class HideLinesIntention(private val setAccessor: (IdeologDocumentContext) -> HashSet<String>) : IntentionAction {
  private var lastSelection = ""
  val shortSelection: String
    get() = if (lastSelection.length > 25) lastSelection.substring(0, 25) + "..." else lastSelection

  override fun getFamilyName() = "Logs"

  fun getText(editor: Editor): CharSequence? {
    val selectionModel = editor.selectionModel
    var selectionStart = selectionModel.selectionStart
    var selectionEnd = selectionModel.selectionEnd


    if (selectionStart == selectionEnd) {
      val doc = editor.document.charsSequence

      while (selectionStart > 0 && doc[selectionStart - 1].isLetterOrDigit())
        selectionStart--

      while (selectionEnd < doc.length && doc[selectionEnd].isLetterOrDigit())
        selectionEnd++
    }

    if (selectionEnd - selectionStart > 100 || selectionEnd == selectionStart)
      return null

    return editor.document.getText(TextRange(selectionStart, selectionEnd))
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
