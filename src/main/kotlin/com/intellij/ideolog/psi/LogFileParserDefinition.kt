package com.intellij.ideolog.psi

class LogFileParserDefinition : com.intellij.lang.ParserDefinition {
  companion object {
    val LOG_FILE_ELEMENT_TYPE = com.intellij.psi.tree.IFileElementType(com.intellij.ideolog.fileType.LogLanguage)
  }

  override fun createLexer(project: com.intellij.openapi.project.Project?): com.intellij.lexer.Lexer {
    return com.intellij.lexer.EmptyLexer()
  }

  override fun createParser(project: com.intellij.openapi.project.Project?): com.intellij.lang.PsiParser {
    throw UnsupportedOperationException("Not supported")
  }

  override fun getFileNodeType() = LOG_FILE_ELEMENT_TYPE

  override fun getWhitespaceTokens() = com.intellij.psi.tree.TokenSet.EMPTY!!

  override fun getCommentTokens() = com.intellij.psi.tree.TokenSet.EMPTY!!

  override fun getStringLiteralElements() = com.intellij.psi.tree.TokenSet.EMPTY!!

  override fun createElement(node: com.intellij.lang.ASTNode?) = com.intellij.psi.util.PsiUtilCore.NULL_PSI_ELEMENT!!

  override fun createFile(fileViewProvider: com.intellij.psi.FileViewProvider) = LogPsiFile(fileViewProvider) // LogPsiFileStub(fileViewProvider)

  override fun spaceExistanceTypeBetweenTokens(left: com.intellij.lang.ASTNode?, right: com.intellij.lang.ASTNode?) = com.intellij.lang.ParserDefinition.SpaceRequirements.MAY
}

