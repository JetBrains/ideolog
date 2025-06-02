package com.intellij.ideolog.foldings

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.file.LogFileEditorProvider
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil

class LogFoldingTest : BasePlatformTestCase() {
  private lateinit var editor: LogFileEditor

  override fun tearDown() {
    try {
      if (::editor.isInitialized) {
        Disposer.dispose(editor)
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun testFoldingCalculation() {
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
    assertFalse("Fold regions should be created", foldRegions.isEmpty())

    val foldedText = getFoldedText(foldRegions)
    assertTrue("Folded text should contain INFO lines", foldedText.contains("INFO"))

    val visibleText = getVisibleText(editor.editor.document.text, foldRegions)
    assertTrue("Visible text should contain ERROR lines", visibleText.contains("ERROR"))
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
