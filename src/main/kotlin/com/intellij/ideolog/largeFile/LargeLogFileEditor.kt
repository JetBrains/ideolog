package com.intellij.ideolog.largeFile

import com.intellij.ideolog.IdeologBundle
import com.intellij.largeFilesEditor.editor.LargeFileEditor

class LargeLogFileEditor(delegate: LargeFileEditor) : LargeFileEditor by delegate {
  override fun getName() = IdeologBundle.message("large.log")

  init {
    editor.settings.isUseSoftWraps = false
    this.editor.putUserData(LargeFileEditor.LARGE_FILE_EDITOR_SOFT_WRAP_KEY, false)
  }
}
