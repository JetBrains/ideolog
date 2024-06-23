package com.intellij.ideolog.terminal

import com.intellij.ideolog.util.IdeologContextDetector
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isOutputEditor

private val documentContextKey = Key.create<IdeologDocumentContext>("IdeologTerminalDocumentContext")

class IdeologTerminalContextDetector : IdeologContextDetector {
  override fun detectIdeologContext(editor: Editor): IdeologDocumentContext {
    return with(editor.document) {
      getUserData(documentContextKey) ?: run {
        putUserData(documentContextKey, IdeologTerminalDocumentContext(this))
        getUserData(documentContextKey)!!
      }
    }
  }

  override fun isApplicable(editor: Editor): Boolean = editor.isOutputEditor
}
