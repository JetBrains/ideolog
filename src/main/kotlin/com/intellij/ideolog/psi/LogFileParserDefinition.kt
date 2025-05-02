package com.intellij.ideolog.psi

import com.intellij.ideolog.fileType.LogLanguage
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore

class LogFileParserDefinition : ParserDefinition {

  override fun createLexer(project: Project?): Lexer {
    return EmptyLexer()
  }

  override fun createParser(project: Project?): PsiParser {
    throw UnsupportedOperationException("Not supported")
  }

  override fun getFileNodeType(): IFileElementType = LOG_FILE_ELEMENT_TYPE

  override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY

  override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode?): PsiElement = PsiUtilCore.NULL_PSI_ELEMENT

  override fun createFile(fileViewProvider: FileViewProvider): LogPsiFile = LogPsiFile(fileViewProvider) // LogPsiFileStub(fileViewProvider)
}

private val LOG_FILE_ELEMENT_TYPE = IFileElementType(LogLanguage)
