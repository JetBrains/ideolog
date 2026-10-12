package com.intellij.ideolog.textmate.highlighting

import com.intellij.ideolog.highlighting.CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT
import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightModuleFixture
import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestApplication
@TestDataPath($$"$PROJECT_ROOT/plugins/ideolog/textmate/testResources/highlighting")
class LogEditorHighlighterTest {
  companion object {
    private val projectFixture = projectFixture(openAfterCreation = true)
  }

  private val myFixture by codeInsightModuleFixture(projectFixture)
  private lateinit var testName: String

  @BeforeEach
  fun setUp(testInfo: TestInfo) {
    testName = testInfo.testMethod.get().name.removePrefix("test")
  }

  @Test
  fun testLogFileNonExistingFormat(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      configureByFile()
      val highlighter = assertInstanceOf(LogEditorHighlighter::class.java, myFixture.editor.highlighter)
      assertInstanceOf(LexerEditorHighlighter.HighlighterIteratorImpl::class.java, highlighter.createIterator(0))
    }
  }


  @Test
  fun testBigLogFileNonExistingFormat(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      configureByText("a".repeat(CUSTOM_DEFAULT_LOG_HIGHLIGHTER_SIZE_CONSTRAINT) + 1)
      val highlighter = assertInstanceOf(LogEditorHighlighter::class.java, myFixture.editor.highlighter)
      assertFalse(highlighter.createIterator(0) is LexerEditorHighlighter.HighlighterIteratorImpl)
    }
  }

  private fun configureByFile(): PsiFile {
    return myFixture.configureByFile("$testName.log")
  }

  private fun configureByText(text: String): PsiFile {
    return myFixture.configureByText("$testName.log", text)
  }
}
