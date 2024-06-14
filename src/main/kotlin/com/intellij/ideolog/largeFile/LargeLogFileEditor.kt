package com.intellij.ideolog.largeFile

import com.intellij.largeFilesEditor.editor.LargeFileEditor

class LargeLogFileEditor(delegate: LargeFileEditor) : LargeFileEditor by delegate {
  override fun getName() = "Logos Large"

  init {
    editor.settings.isUseSoftWraps = false
    this.editor.putUserData(LargeFileEditor.LARGE_FILE_EDITOR_SOFT_WRAP_KEY, false)
  }
}
