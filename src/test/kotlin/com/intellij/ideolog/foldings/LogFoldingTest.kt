package com.intellij.ideolog.foldings

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.file.LogFileEditorProvider
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.util.Disposer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightModuleFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@TestApplication
class LogFoldingTest {
  companion object {
    private val projectFixture = projectFixture(openAfterCreation = true)
  }

  private val myFixture by codeInsightModuleFixture(projectFixture)
  private val project get() = myFixture.project
  private lateinit var editor: LogFileEditor

  @Test
  fun testFoldingCalculation(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      try {
        doTestFoldingCalculation()
      }
      finally {
        if (::editor.isInitialized) {
          Disposer.dispose(editor)
        }
      }
    }
  }

  private fun doTestFoldingCalculation() {
    val logContent = """
            2023-05-02 23:09:07,110 [    142]   INFO - #c.i.i.StartupUtil - JVM: 17.0.3+7-b469.32 (OpenJDK 64-Bit Server VM)
            2023-05-02 23:09:07,118 [    150]   INFO - #c.i.i.StartupUtil - PID: 15672
            2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - args: []
            2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - library path: C:\\Users\\user\\path\\to\\library
            2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - boot library path: C:\\Users\\user\\path\\to\\boot\\library
            2023-05-02 23:09:07,133 [    165]   ERROR - #c.i.i.StartupUtil - Some error occurred
            2023-05-02 23:09:07,161 [    193]   INFO - #c.i.i.StartupUtil - CPU cores: 32
            2023-05-02 23:09:07,312 [    344]   INFO - #c.i.i.p.PluginManager - Plugin PluginDescriptor loaded
            2023-05-02 23:09:07,312 [    344]   INFO - #c.i.i.p.PluginManager - Plugin PluginDescriptor loaded again
            2023-05-02 23:09:07,417 [    449]   ERROR - #c.i.i.p.PluginManager - Module intellij.space.gateway is not enabled
        """.trimIndent()

    val file = myFixture.configureByText("test.log", logContent)
    editor = LogFileEditorProvider().createEditor(project, file.virtualFile) as LogFileEditor

    val context = editor.editor.document.ideologContext
    context.hiddenSubstrings.add("INFO")
    FoldingCalculatorTask.restartFoldingCalculator(project, editor.editor, file)
    UIUtil.dispatchAllInvocationEvents()

    val foldRegions = editor.editor.foldingModel.allFoldRegions
    assertFalse(foldRegions.isEmpty(), "Fold regions should be created")

    val foldedText = getFoldedText(foldRegions)
    assertTrue(foldedText.contains("INFO"), "Folded text should contain INFO lines")

    val visibleText = getVisibleText(editor.editor.document.text, foldRegions)
    assertTrue(visibleText.contains("ERROR"), "Visible text should contain ERROR lines")
  }

  private fun getFoldedText(foldRegions: Array<FoldRegion>): String {
    val sb = StringBuilder()
    for (region in foldRegions) {
      sb.append(region.document.getText(region.textRange))
      sb.append("\n")
    }
    return sb.toString()
  }

  private fun getVisibleText(fullText: String, foldRegions: Array<FoldRegion>): String {
    val foldedRanges = foldRegions.map { it.startOffset..it.endOffset }.toSet()
    return fullText.filterIndexed { index, _ -> foldedRanges.none { index in it } }
  }
}
