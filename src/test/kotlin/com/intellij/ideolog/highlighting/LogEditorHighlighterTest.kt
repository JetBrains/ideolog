package com.intellij.ideolog.highlighting

import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/resources/highlighting")
class LogEditorHighlighterTest: BasePlatformTestCase() {
  override fun getTestDataPath(): String = "resources/highlighting"

  fun testDetectHighlighterForLogFile() {
    myFixture.configureByText("LogFile.log", "")
    assertInstanceOf(myFixture.editor.highlighter, LogEditorHighlighter::class.java)
  }

  fun testLogFileExistingFormat() {
    configureByFile()
    val highlighter = myFixture.editor.highlighter
    assertInstanceOf(highlighter, LogEditorHighlighter::class.java)
    assertInstanceOf(highlighter.createIterator(0), LogHighlightingIterator::class.java)
  }

  private fun configureByFile(): PsiFile {
    return myFixture.configureByFile(getTestName(false) + ".log")
  }
}
