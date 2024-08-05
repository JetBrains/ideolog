package com.intellij.ideolog.textmate.highlighting

import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/testResources/highlighting")
class LogEditorHighlighterTest: BasePlatformTestCase() {
  override fun getTestDataPath(): String = "testResources/highlighting"

  fun testLogFileNonExistingFormat() {
    configureByFile()
    val highlighter = myFixture.editor.highlighter
    assertInstanceOf(highlighter, LogEditorHighlighter::class.java)
    assertInstanceOf((highlighter as LogEditorHighlighter).createIterator(0),
                     LexerEditorHighlighter.HighlighterIteratorImpl::class.java)
  }

  private fun configureByFile(): PsiFile {
    return myFixture.configureByFile(getTestName(false) + ".log")
  }
}
