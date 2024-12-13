package com.intellij.ideolog.highlighting.ui

import com.intellij.codeInsight.editorLineStripeHint.EditorLineStripeHintComponent
import com.intellij.execution.impl.InlayProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface EditorLineStripeHintComponentBuilderProvider {
  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<EditorLineStripeHintComponentBuilderProvider> =
      ExtensionPointName("com.intellij.ideolog.editorLineStripeHintComponentBuilderProvider")
  }

  fun getBuilder(project: Project): EditorLineStripeHintComponentBuilder
}

interface EditorLineStripeHintComponentBuilder {
  fun build(inlayProvider: InlayProvider?, editor: Editor, offset: Int): EditorLineStripeHintComponent
}
