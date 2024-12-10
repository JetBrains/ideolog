package com.intellij.ideolog.highlighting

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.file.LogFileEditorProvider
import com.intellij.ideolog.highlighting.settings.DefaultSettingsStoreItems
import com.intellij.ideolog.highlighting.settings.LogHighlightingPattern
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.math.abs

class ErrorStripeTest : BasePlatformTestCase() {
  private lateinit var editor: LogFileEditor
  private lateinit var highlightingPatternsBackup: List<LogHighlightingPattern>

  override fun setUp() {
    super.setUp()
    highlightingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.patterns.map { it.copy() }
  }

  override fun tearDown() {
    try {
      LogHighlightingSettingsStore.getInstance().myState.patterns.clear()
      LogHighlightingSettingsStore.getInstance().myState.patterns.addAll(highlightingPatternsBackup)
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
  override fun getTestDataPath(): String =
    Path.of(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins/ideolog/src/test/resources/highlighting/errorStripe").pathString

  fun testUndetectedFormat() {
    val markupModel = createMarkupModel()
    val format = markupModel.document.ideologContext.detectLogFileFormat()
    assertNull(format.myRegexLogParser?.uuid)

    val highlighters = markupModel.allHighlighters.map { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertSize(1, highlighters)
  }

  fun testFirstNonStripedEventLineNotOnStripe() {
    val highlighters = calculateHighlighters()
    val endOfFirstLine = editor.editor.document.getLineEndOffset(0)
    val visibleHighlighters = highlighters.filter { it.startOffset > endOfFirstLine + 1 }.mapNotNull { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertSize(1, visibleHighlighters)
  }

  fun testFirstStripedEventLineNotOnStripe() {
    val highlighters = calculateHighlighters()
    val endOfFirstLine = editor.editor.document.getLineEndOffset(0)
    val visibleHighlighters = highlighters.filter { it.startOffset > endOfFirstLine + 1 }.mapNotNull { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertSize(1, visibleHighlighters)
  }

  fun testNoStripedEvents() {
    val highlighters = calculateUniqueHighlighters()
    assertSize(1, highlighters)
  }

  fun testDefaultErrorStripedEvents() {
    val highlighters = calculateUniqueHighlighters()
    assertSize(2, highlighters)
  }

  fun testCustomStripedEvents() {
    assertTrue(LogHighlightingSettingsStore.getInstance().myState.patterns.removeIf {
      it.uuid.toString() == "11ff1574-2118-4722-905a-61bec89b079e"
    })
    LogHighlightingSettingsStore.getInstance().myState.patterns.add(DefaultSettingsStoreItems.Warning.copy(showOnStripe = true))
    val highlighters = calculateUniqueHighlighters()
    assertSize(2, highlighters)
  }

  private fun calculateHighlighters(): Array<out RangeHighlighter> {
    val markupModel = createMarkupModel()
    return markupModel.allHighlighters
  }

  private fun calculateUniqueHighlighters(): Set<Color?> {
    val markupModel = createMarkupModel()
    return markupModel.allHighlighters.mapNotNull { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
  }

  private fun createMarkupModel(): MarkupModel {
    val file = myFixture.copyFileToProject(getTestName(false) + ".log")
    editor = LogFileEditorProvider().createEditor(project, file) as LogFileEditor
    val mapRenderer = LogFileMapRenderer.getLogFileMapRenderer(editor.editor)
    Thread.sleep(1000)
    NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
    UIUtil.dispatchAllInvocationEvents()
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    assertNotNull(mapRenderer)
    val markupModel = editor.editor.markupModel
    assertEquals(1024, markupModel.allHighlighters.size)
    return markupModel
  }

  private fun Color.toTimeIndependent(): Color = Color(abs(this.red - this.blue), 0, 0)
}
