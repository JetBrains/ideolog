package com.intellij.ideolog.psi

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.psi.LogFileTokenTypes.LOG_CONTENT_FILE
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLargeFile
import com.intellij.psi.impl.source.PsiFileImpl


class LogPsiFile(fileViewProvider: FileViewProvider) :
  PsiFileImpl(LOG_CONTENT_FILE, LOG_CONTENT_FILE, fileViewProvider), PsiLargeFile, NavigatableWithText {
  override fun getNavigateActionText(focusEditor: Boolean): String {
    return IdeologBundle.message("action.navigate.to.source.text")
  }

  override fun getFileType(): LogFileType = LogFileType

  override fun accept(visitor: PsiElementVisitor) {}

  override fun isWritable(): Boolean = true
}

