package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class LogThisIntentionBase : IntentionAction {
    private var lastText: String = ""
    protected val fieldText: String
      get() = lastText
    abstract override fun getText(): String

    override fun getFamilyName(): String {
      return IdeologBundle.message("intention.family.name.logs")
    }

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile?): Boolean {
      if (psiFile?.fileType != LogFileType)
        return false

      val columnCount = LogParsingUtils.getEventColumnCount(editor)
      val currentColumn = LogParsingUtils.getColumnByOffset(editor)

      val visible = currentColumn >= 0 && currentColumn != columnCount - 1
      if (visible) {
        val columnValue = LogParsingUtils.getColumnValueByOffset(editor) ?: "?"
        lastText = columnValue.toString().trim()
      }

      return visible
    }

    abstract fun getIntentionItems(editor: Editor): HashSet<Pair<Int, String>>

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile?) {
      val set = getIntentionItems(editor)
      val currentColumn = LogParsingUtils.getColumnByOffset(editor)
      val columnValue = LogParsingUtils.getColumnValueByOffset(editor) ?: "?"

      set.add(currentColumn to columnValue.toString())

      FoldingCalculatorTask.restartFoldingCalculator(project, editor, psiFile)
    }

    override fun startInWriteAction(): Boolean {
      return false
    }
}
