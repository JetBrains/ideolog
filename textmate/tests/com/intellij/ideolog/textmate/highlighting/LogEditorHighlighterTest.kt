package com.intellij.ideolog.textmate.highlighting

import com.intellij.ideolog.highlighting.CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT
import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import java.nio.file.Path
import kotlin.io.path.pathString

class LogEditorHighlighterTest: BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    Path.of(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins/ideolog/textmate/testResources/highlighting").pathString

  fun testLogFileNonExistingFormat() {
    configureByFile()
    val highlighter = myFixture.editor.highlighter
    assertInstanceOf(highlighter, LogEditorHighlighter::class.java)
    assertInstanceOf((highlighter as LogEditorHighlighter).createIterator(0),
                     LexerEditorHighlighter.HighlighterIteratorImpl::class.java)
  }


  fun testBigLogFileNonExistingFormat() {
    configureByText("a".repeat(CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT) + 1)
    val highlighter = myFixture.editor.highlighter
    assertInstanceOf(highlighter, LogEditorHighlighter::class.java)
    assertFalse((highlighter as LogEditorHighlighter).createIterator(0) is LexerEditorHighlighter.HighlighterIteratorImpl)
  }

  private fun configureByFile(): PsiFile {
    return myFixture.configureByFile(getTestName(false) + ".log")
  }

  private fun configureByText(text: String): PsiFile {
    return myFixture.configureByText(getTestName(false) + ".log", text)
  }
}
