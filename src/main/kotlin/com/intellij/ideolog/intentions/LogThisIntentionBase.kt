package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class LogThisIntentionBase : IntentionAction {
    private var lastText: String = ""
    protected val fieldText get() = lastText
    abstract override fun getText(): String

    override fun getFamilyName(): String {
      return "Logs"
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
      if (file?.fileType != LogFileType)
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

    override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
      val set = getIntentionItems(editor)
      val currentColumn = LogParsingUtils.getColumnByOffset(editor)
      val columnValue = LogParsingUtils.getColumnValueByOffset(editor) ?: "?"

      set.add(currentColumn to columnValue.toString())

      FoldingCalculatorTask.restartFoldingCalculator(project, editor, file)
    }

    override fun startInWriteAction(): Boolean {
      return false
    }
}
