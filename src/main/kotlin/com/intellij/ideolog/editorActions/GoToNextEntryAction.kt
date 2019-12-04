package com.intellij.ideolog.editorActions

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor

class GoToNextEntryAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canExecute(e)
  }

  private fun canExecute(e: AnActionEvent): Boolean {
    val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return false

    return psiFile.fileType == LogFileType
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
    val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return
    val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
    if (!canExecute(e)) return

    val foldingModel = editor.foldingModel
    var event = LogEvent.fromEditor(editor, editor.caretModel.offset)
    var nextPos = event.endOffset + 1
    while (nextPos < editor.document.textLength) {
      if (foldingModel.isOffsetCollapsed(nextPos)) {
        event = LogEvent.fromEditor(editor, nextPos)
        nextPos = event.endOffset + 1
      } else {
        event = LogEvent.fromEditor(editor, nextPos)
        val descriptor = OpenFileDescriptor(project, psiFile.virtualFile, event.startOffset)
        val navigable = descriptor.setUseCurrentWindow(true)
        if (navigable.canNavigate()) navigable.navigate(true)
        return
      }
    }
  }
}
