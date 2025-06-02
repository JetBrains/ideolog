package com.intellij.ideolog.filters

import com.intellij.ideolog.highlighting.LogHeavyFilterService
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil

class LogFilterTest : BasePlatformTestCase() {

  fun testFilterEnqueuing() {
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
    assertSize(1, editor.markupModel.allHighlighters)
  }
}
