package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.foldings.FoldingCalculatorTask
import com.intellij.ideolog.foldings.hiddenItemsKey
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.containers.HashSet

class LogHideThisIntention : IntentionAction {
  var lastText: String = ""

  override fun getText(): String {
    return "Hide lines with '$lastText' in this field"
  }

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

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    val set = editor.document.getUserData(hiddenItemsKey) ?: HashSet()
    val currentColumn = LogParsingUtils.getColumnByOffset(editor)
    val columnValue = LogParsingUtils.getColumnValueByOffset(editor) ?: "?"

    set.add(currentColumn to columnValue.toString())
    editor.document.putUserData(hiddenItemsKey, set)


    lastLaunchedTask?.myCancel = true
    val task = FoldingCalculatorTask(project, editor, file?.name ?: "?")
    lastLaunchedTask = task
    ProgressManager.getInstance().run(task)
  }

  companion object {
    var lastLaunchedTask: FoldingCalculatorTask? = null
  }

  override fun startInWriteAction(): Boolean {
    return false
  }
}
