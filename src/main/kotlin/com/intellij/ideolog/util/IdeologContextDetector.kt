package com.intellij.ideolog.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName

private val EP_NAME: ExtensionPointName<IdeologContextDetector> = ExtensionPointName.create("com.intellij.ideolog.ideologContextDetector")
private val LOG_FILE_FORMAT_DETECTORS = EP_NAME.extensionList

interface IdeologContextDetector {
  fun detectIdeologContext(editor: Editor): IdeologDocumentContext
  fun isApplicable(editor: Editor): Boolean
}

fun detectIdeologContext(editor: Editor): IdeologDocumentContext {
  val applicableLogFileFormatDetectors = LOG_FILE_FORMAT_DETECTORS.filter { detector -> detector.isApplicable(editor) }
  if (applicableLogFileFormatDetectors.isEmpty()) return editor.document.ideologContext
  return applicableLogFileFormatDetectors.first().detectIdeologContext(editor)
}
