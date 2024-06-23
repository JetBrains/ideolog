package com.intellij.ideolog.largeFile

import com.intellij.ideolog.util.IdeologContextDetector
import com.intellij.ideolog.util.IdeologDocumentContext
import com.intellij.largeFilesEditor.editor.LargeFileEditor.LARGE_FILE_EDITOR_KEY
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key

private val documentContextKey = Key.create<IdeologLargeFileDocumentContext>("IdeologLargeFileDocumentContext")

class IdeologLargeFileContextDetector : IdeologContextDetector {
  override fun detectIdeologContext(editor: Editor): IdeologDocumentContext {
    return with(editor.document) {
      getUserData(documentContextKey) ?: run {
        putUserData(documentContextKey, IdeologLargeFileDocumentContext(this))
        getUserData(documentContextKey)!!
      }
    }
  }

  override fun isApplicable(editor: Editor): Boolean = editor.getUserData(LARGE_FILE_EDITOR_KEY) != null
}
