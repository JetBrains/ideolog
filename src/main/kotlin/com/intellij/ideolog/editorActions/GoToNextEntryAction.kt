package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.util.getGoToActionContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor

class GoToNextEntryAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.dataContext.getData(CommonDataKeys.PSI_FILE)?.fileType == LogFileType
  }

  override fun actionPerformed(e: AnActionEvent) {
    val ctx = e.getGoToActionContext() ?: return
    var nextPos = ctx.event.endOffset + 1
    while (nextPos < ctx.editor.document.textLength) {
      if (ctx.foldingModel.isOffsetCollapsed(nextPos)) {
        val event = LogEvent.fromEditor(ctx.editor, nextPos)
        nextPos = event.endOffset + 1
      } else {
        val event = LogEvent.fromEditor(ctx.editor, nextPos)
        val descriptor = OpenFileDescriptor(ctx.project, ctx.psiFile.virtualFile, event.startOffset)
        val navigable = descriptor.setUseCurrentWindow(true)
        if (navigable.canNavigate()) navigable.navigate(true)
        return
      }
    }
  }
}
