package com.intellij.ideolog.editorActions

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.ideolog.highlighting.LogParsingUtils
import com.intellij.ideolog.lex.LogToken
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.ideolog.psi.LogPsiFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.*


class ExtendsSelection : ExtendWordSelectionHandlerBase() {
  override fun canSelect(e: PsiElement): Boolean {
    return e is LogPsiFile || e.parent is LogPsiFile
  }

  override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
    val (evt, evtOffset) = LogParsingUtils.getEvent(editor, editor.selectionModel.selectionStart)

    val fileType = detectLogFileFormat(editor)
    val tokens = ArrayList<LogToken>()
    fileType.tokenize(evt, tokens, true)

    for ((startOffset, endOffset) in tokens) {
      if (evtOffset + startOffset < editor.selectionModel.selectionStart && evtOffset + endOffset > editor.selectionModel.selectionEnd) {
        return listOf(TextRange(evtOffset + startOffset, evtOffset + endOffset))
      }
    }

    return listOf(TextRange(evtOffset, evtOffset + evt.length))
  }
}
