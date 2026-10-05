package com.intellij.ideolog.highlighting

import com.intellij.openapi.application.EDT
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightProjectFixture
import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@TestApplication
@TestDataPath($$"$PROJECT_ROOT/plugins/ideolog/src/test/resources/highlighting")
class LogEditorHighlighterTest {
  companion object {
    private val projectFixture = codeInsightProjectFixture()
  }

  private val myFixture by codeInsightFixture(projectFixture)

  @Test
  fun testDetectHighlighterForLogFile(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.configureByText("LogFile.log", "")
      assertInstanceOf(LogEditorHighlighter::class.java, myFixture.editor.highlighter)
    }
  }

  @Test
  fun testLogFileExistingFormat(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      configureByFile()
      val highlighter = myFixture.editor.highlighter
      assertInstanceOf(LogEditorHighlighter::class.java, highlighter)
      assertInstanceOf(LogHighlightingIterator::class.java, highlighter.createIterator(0))
    }
  }

  private fun configureByFile(): PsiFile {
    return myFixture.configureByFile("LogFileExistingFormat.log")
  }
}
