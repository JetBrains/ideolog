package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.ideolog.highlighting.highlightTimeKey
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ToggleTimeHighlightingIntention : IntentionAction {
  override fun getText(): String {
    return IdeologBundle.message("intention.name.toggle.time.highlighting")
  }

  override fun getFamilyName(): String = IdeologBundle.message("intention.family.name.logs")

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile?): Boolean {
    if (psiFile?.fileType != LogFileType)
      return false

    val fileType = detectLogFileFormat(editor)

    val columnCount = LogParsingUtils.getEventColumnCount(editor)
    val currentColumn = LogParsingUtils.getColumnByOffset(editor)

    return columnCount > 1 && currentColumn == fileType.getTimeFieldIndex() && currentColumn >= 0
  }

  override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
    editor ?: return
    val newValue = !(editor.getUserData(highlightTimeKey) ?: false)
    editor.putUserData(highlightTimeKey, newValue)
    (editor as EditorEx).repaint(0, editor.document.textLength)

    LogFileMapRenderer.getLogFileMapRenderer(editor)?.setIsRenderingTimeHighlighting(newValue)
  }

  override fun startInWriteAction(): Boolean = false
}
