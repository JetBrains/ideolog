package com.intellij.ideolog.fileType

import com.intellij.ideolog.file.LogIcons

object LogFileType : com.intellij.openapi.fileTypes.LanguageFileType(LogLanguage) {
  override fun getName(): String = "Log"
  override fun getDescription(): String = "Log files"
  override fun getDefaultExtension(): String = "log"
  override fun getIcon(): javax.swing.Icon = LogIcons.LogFile
}
