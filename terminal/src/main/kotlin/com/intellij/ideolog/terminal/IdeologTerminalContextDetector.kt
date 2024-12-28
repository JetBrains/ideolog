package com.intellij.ideolog.terminal

import com.intellij.ideolog.util.IdeologContextDetector
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isOutputEditor

private val documentContextKey = Key.create<IdeologDocumentContext>("IdeologTerminalDocumentContext")

private class IdeologTerminalContextDetector : IdeologContextDetector {
  override fun detectIdeologContext(editor: Editor): IdeologDocumentContext {
    return with(editor.document) {
      getUserData(documentContextKey) ?: run {
        putUserData(documentContextKey, IdeologTerminalDocumentContext(this))
        getUserData(documentContextKey)!!
      }
    }
  }

  override fun isApplicable(editor: Editor): Boolean = editor.isOutputEditor &&
                                                       Registry.`is`("ideolog.terminal.enabled")
}
