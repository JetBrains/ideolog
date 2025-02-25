package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.util.getGoToActionContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor

class GoToPrevEntryAction : AnAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.dataContext.getData(CommonDataKeys.PSI_FILE)?.fileType == LogFileType
  }

  override fun actionPerformed(e: AnActionEvent) {
    val ctx = e.getGoToActionContext() ?: return
    var prevPos = ctx.event.startOffset - 1
    while (prevPos >= 0) {
      val prevEvent = LogEvent.fromEditor(ctx.editor, prevPos)
      if (ctx.foldingModel.isOffsetCollapsed(prevEvent.startOffset)) {
        val event = LogEvent.fromEditor(ctx.editor, prevPos)
        prevPos = event.startOffset - 1
      } else {
        val descriptor = OpenFileDescriptor(ctx.project, ctx.psiFile.virtualFile, prevEvent.startOffset)
        val navigable = descriptor.setUseCurrentWindow(true)
        if (navigable.canNavigate()) navigable.navigate(true)
        return
      }
    }
  }
}
