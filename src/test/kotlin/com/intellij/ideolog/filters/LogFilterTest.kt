package com.intellij.ideolog.filters

import com.intellij.ideolog.highlighting.LogHeavyFilterService
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.TextRange
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightProjectFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@TestApplication
@Disabled("AT-4013")
class LogFilterTest {
  companion object {
    private val projectFixture = codeInsightProjectFixture()
  }

  private val myFixture by codeInsightFixture(projectFixture)
  private val project get() = myFixture.project

  @Test
  fun testFilterEnqueuing(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      doTestFilterEnqueuing()
    }
  }

  private fun doTestFilterEnqueuing() {
    val logContent = """
            2023-05-02 23:09:07,110 [    142]   INFO - #c.i.i.StartupUtil - Check out https://www.jetbrains.com for more information
            2023-05-02 23:09:07,118 [    150]   INFO - #c.i.i.StartupUtil - File path: C:\Users\user\path\to\file.txt
            2023-05-02 23:09:07,133 [    165]   ERROR - #c.i.i.StartupUtil - Exception in thread "main" java.lang.NullPointerException
                at com.example.MyClass.method(MyClass.java:123)
                at com.example.OtherClass.otherMethod(OtherClass.java:45)
        """.trimIndent()

    myFixture.configureByText("test.log", logContent)
    val editor = myFixture.editor

    val service = LogHeavyFilterService.getInstance(project)

    val firstLine = editor.document.getText(TextRange(
      editor.document.getLineStartOffset(0),
      editor.document.getLineEndOffset(0)
    ))

    service.enqueueHeavyFiltering(editor, 0, firstLine)
    UIUtil.dispatchAllInvocationEvents()
    assertEquals(1, editor.markupModel.allHighlighters.size)
  }
}
