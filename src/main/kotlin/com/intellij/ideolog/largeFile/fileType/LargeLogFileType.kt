package com.intellij.ideolog.largeFile.fileType

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.file.LogIcons

object LargeLogFileType : com.intellij.openapi.fileTypes.LanguageFileType(LargeLogLanguage) {
  override fun getName(): String = "LargeLog"
  override fun getDescription(): String = IdeologBundle.message("label.large.log.files")
  override fun getDefaultExtension(): String = "log"
  override fun getIcon(): javax.swing.Icon = LogIcons.LogFile
}
