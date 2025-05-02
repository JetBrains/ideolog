package com.intellij.ideolog.highlighting

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import java.nio.file.Path
import kotlin.io.path.pathString

class LogEditorHighlighterTest: BasePlatformTestCase() {
  override fun getTestDataPath(): String {
    val platformTestDataPath = Path.of(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins/ideolog/src/test/resources/highlighting")
    if (platformTestDataPath.toFile().exists()) {
      return platformTestDataPath.pathString
    }
    return "src/test/resources/highlighting"
  }

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
