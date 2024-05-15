package com.intellij.ideolog.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName

private val EP_NAME: ExtensionPointName<IdeologContextDetector> = ExtensionPointName.create("com.intellij.ideolog.ideologContextDetector")
private val logFileFormatDetectors = EP_NAME.extensionList

interface IdeologContextDetector {
  fun detectIdeologContext(editor: Editor): IdeologDocumentContext
}

fun detectIdeologContext(editor: Editor): IdeologDocumentContext {
  if (logFileFormatDetectors.isEmpty()) return editor.document.ideologContext
  return logFileFormatDetectors.first().detectIdeologContext(editor)
}
