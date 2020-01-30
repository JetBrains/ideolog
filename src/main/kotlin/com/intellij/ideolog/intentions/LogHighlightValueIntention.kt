package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.ideolog.highlighting.highlightingSetUserKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.util.*

class LogHighlightValueIntention : IntentionAction {
  var lastSelection = ""
  override fun getText(): String {
    return "Highlight '${if (lastSelection.length > 25) lastSelection.substring(0, 25) + "..." else lastSelection}'"
  }

  override fun getFamilyName() = "Logs"

  fun getText(editor: Editor): CharSequence? {
    val selectionModel = editor.selectionModel
    var selectionStart = selectionModel.selectionStart
    var selectionEnd = selectionModel.selectionEnd


    if (selectionStart == selectionEnd) {
      val doc = editor.document.charsSequence

      while (selectionStart > 0 && doc[selectionStart - 1].isLetterOrDigit())
        selectionStart--

      while (selectionEnd < doc.length && doc[selectionEnd].isLetterOrDigit())
        selectionEnd++
    }

    if (selectionEnd - selectionStart > 100 || selectionEnd == selectionStart)
      return null

    return editor.document.getText(TextRange(selectionStart, selectionEnd))
  }

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile?): Boolean {
    if (file?.fileType != LogFileType)
      return false

    val text = getText(editor)
    val enabled = text != null
    if (enabled)
      lastSelection = text.toString()
    return enabled

  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    val selection = getText(editor) ?: return

    val set = editor.getUserData(highlightingSetUserKey) ?: HashSet()
    set.add(selection.toString())
    editor.putUserData(highlightingSetUserKey, set)
    (editor as EditorEx).repaint(0, editor.document.textLength)
    LogFileMapRenderer.getLogFileMapRenderer(editor)?.invalidateHighlighters()
  }

  override fun startInWriteAction() = false
}
