package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor

class GoToPrevEntryAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
    val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return
    val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val enabled = psiFile.fileType == LogFileType
    e.presentation.isEnabled = enabled
    if (enabled) {
      val foldingModel = editor.foldingModel
      var event = LogEvent.fromEditor(editor, editor.caretModel.offset)
      var prevPos = event.startOffset - 1
      while (prevPos >= 0) {
        val prevEvent = LogEvent.fromEditor(editor, prevPos)
        if (foldingModel.isOffsetCollapsed(prevEvent.startOffset)) {
          event = LogEvent.fromEditor(editor, prevPos)
          prevPos = event.startOffset - 1
        } else {
          val descriptor = OpenFileDescriptor(project, psiFile.virtualFile, prevEvent.startOffset)
          val navigable = descriptor.setUseCurrentWindow(true)
          if (navigable.canNavigate()) navigable.navigate(true)
          return
        }
      }
    }
  }
}
