package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.intentions.LogJumpToSourceIntention
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class JumpToSourceAction : AnAction(){
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)?: return
        val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE)?: return
        val project = e.dataContext.getData(CommonDataKeys.PROJECT)?: return


        val enabled = psiFile.fileType == LogFileType
        e.presentation.isEnabled = enabled
        if (enabled) {
            LogJumpToSourceIntention.doIt(project, editor)
        }
    }

}
