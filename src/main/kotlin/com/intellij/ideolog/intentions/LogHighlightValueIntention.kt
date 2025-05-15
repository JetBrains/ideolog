package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.highlightingSetUserKey
import com.intellij.ideolog.util.getSelectedText
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class LogHighlightValueIntention : IntentionAction {
  private var lastSelection = ""
  override fun getText(): String {
    return IdeologBundle.message("intention.name.highlight", if (lastSelection.length > 25) lastSelection.substring(0, 25) + "..." else lastSelection)
  }

  override fun getFamilyName(): String = IdeologBundle.message("intention.family.name.logs")

  fun getText(editor: Editor): CharSequence? {
    return editor.getSelectedText()
  }

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile?): Boolean {
    if (psiFile?.fileType != LogFileType)
      return false

    val text = getText(editor)
    val enabled = text != null
    if (enabled)
      lastSelection = text.toString()
    return enabled

  }

  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile?) {
    val selection = getText(editor) ?: return

    val set = editor.getUserData(highlightingSetUserKey) ?: HashSet()
    set.add(selection.toString())
    editor.putUserData(highlightingSetUserKey, set)
    (editor as EditorEx).repaint(0, editor.document.textLength)
    LogFileMapRenderer.getLogFileMapRenderer(editor)?.invalidateHighlighters()
  }

  override fun startInWriteAction(): Boolean = false
}
