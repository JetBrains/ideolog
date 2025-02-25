package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.intentions.LogJumpToSourceIntention
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class JumpToSourceAction : AnAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canExecute(e)
  }

  private fun canExecute(e: AnActionEvent): Boolean {
    val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return false

    return psiFile.fileType == LogFileType
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
    val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return

    if(!canExecute(e))
      return

    LogJumpToSourceIntention.doIt(project, editor)
  }

}
