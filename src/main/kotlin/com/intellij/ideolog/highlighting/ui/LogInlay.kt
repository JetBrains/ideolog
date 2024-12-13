package com.intellij.ideolog.highlighting.ui

import com.intellij.codeInsight.editorLineStripeHint.EditorLineStripeHintComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

internal data class LogInlay(
  private val endOfLinePanel: EditorLineStripeHintComponent,
) : Disposable {
  init {
    Disposer.register(this, endOfLinePanel)
    endOfLinePanel.redraw()
  }

  override fun dispose() {
    endOfLinePanel.uninstall()
  }
}
