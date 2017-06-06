package com.intellij.ideolog.psi

import com.intellij.ideolog.fileType.LogLanguage

object LogFileTokenTypes {
  val LOG_CONTENT_FILE: com.intellij.psi.tree.IElementType = object : com.intellij.psi.tree.IFileElementType("LOG_CONTENT_FILE", com.intellij.ideolog.fileType.LogLanguage) {
    override fun parseContents(chameleon: com.intellij.lang.ASTNode): com.intellij.lang.ASTNode {
      return com.intellij.lang.ASTFactory.leaf(com.intellij.ideolog.psi.LogFileTokenTypes.LOG_CONTENT, chameleon.chars)
    }
    }

    val LOG_CONTENT = com.intellij.psi.tree.IElementType("LOG_CONTENT", LogLanguage)
}
