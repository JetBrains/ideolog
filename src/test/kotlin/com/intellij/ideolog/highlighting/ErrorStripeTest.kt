package com.intellij.ideolog.highlighting

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.file.LogFileEditorProvider
import com.intellij.ideolog.highlighting.settings.DefaultSettingsStoreItems
import com.intellij.ideolog.highlighting.settings.LogHighlightingPattern
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightModuleFixture
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.Color
import kotlin.math.abs

@TestApplication
@TestDataPath($$"$PROJECT_ROOT/plugins/ideolog/src/test/resources/highlighting/errorStripe")
class ErrorStripeTest {
  companion object {
    private val projectFixture = projectFixture(openAfterCreation = true)
  }

  private val myFixture by codeInsightModuleFixture(projectFixture)
  private val project get() = myFixture.project
  private lateinit var editor: LogFileEditor
  private lateinit var highlightingPatternsBackup: List<LogHighlightingPattern>
  private lateinit var errorStripeModeBackup: String
  private lateinit var testName: String

  @BeforeEach
  fun setUp(testInfo: TestInfo) {
    testName = testInfo.testMethod.get().name.removePrefix("test")
    highlightingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.patterns.map { it.copy() }
    errorStripeModeBackup = LogHighlightingSettingsStore.getInstance().myState.errorStripeMode
    LogHighlightingSettingsStore.getInstance().myState.patterns.forEach { highlightingPattern ->
      if (highlightingPattern.uuid == DefaultSettingsStoreItems.Error.uuid) {
        highlightingPattern.showOnStripe = true
      }
    }
    LogHighlightingSettingsStore.getInstance().myState.errorStripeMode = "heatmap"
  }

  @AfterEach
  fun tearDown() {
    // setUp can fail before the backups are made; then the store must stay as it is
    if (::highlightingPatternsBackup.isInitialized) {
      LogHighlightingSettingsStore.getInstance().myState.patterns.clear()
      LogHighlightingSettingsStore.getInstance().myState.patterns.addAll(highlightingPatternsBackup)
    }
    if (::errorStripeModeBackup.isInitialized) {
      LogHighlightingSettingsStore.getInstance().myState.errorStripeMode = errorStripeModeBackup
    }
  }

  @Test
  fun testUndetectedFormat(): Unit = onEdt {
    val markupModel = createMarkupModel()
    val format = markupModel.document.ideologContext.detectLogFileFormat()
    assertNull(format.myRegexLogParser?.uuid)

    val highlighters = markupModel.allHighlighters.map { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertEquals(1, highlighters.size)
  }

  @Test
  fun testFirstNonStripedEventLineNotOnStripe(): Unit = onEdt {
    val highlighters = calculateHighlighters()
    val endOfFirstLine = editor.editor.document.getLineEndOffset(0)
    val visibleHighlighters = highlighters.filter { it.startOffset > endOfFirstLine + 1 }.mapNotNull { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertEquals(1, visibleHighlighters.size)
  }

  @Test
  fun testFirstStripedEventLineNotOnStripe(): Unit = onEdt {
    val highlighters = calculateHighlighters()
    val endOfFirstLine = editor.editor.document.getLineEndOffset(0)
    val visibleHighlighters = highlighters.filter { it.startOffset > endOfFirstLine + 1 }.mapNotNull { highlighter ->
      highlighter.getTextAttributes(editor.editor.colorsScheme)?.errorStripeColor?.toTimeIndependent()
    }.toSet()
    assertEquals(1, visibleHighlighters.size)
  }

  @Test
  fun testNoStripedEvents(): Unit = onEdt {
    val highlighters = calculateUniqueHighlighters()
    assertEquals(1, highlighters.size)
  }

  @Test
  fun testDefaultErrorStripedEvents(): Unit = onEdt {
    val highlighters = calculateUniqueHighlighters()
    assertEquals(2, highlighters.size)
  }

  @Test
  fun testCustomStripedEvents(): Unit = onEdt {
    assertTrue(LogHighlightingSettingsStore.getInstance().myState.patterns.removeIf {
      it.uuid.toString() == "11ff1574-2118-4722-905a-61bec89b079e"
    })
    LogHighlightingSettingsStore.getInstance().myState.patterns.add(DefaultSettingsStoreItems.Warning.copy(showOnStripe = true))
    val highlighters = calculateUniqueHighlighters()
    assertEquals(2, highlighters.size)
  }

  private fun onEdt(block: () -> Unit): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      try {
        block()
      }
      finally {
        if (::editor.isInitialized) {
          Disposer.dispose(editor)
        }
      }
    }
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
    val file = myFixture.copyFileToProject("$testName.log")
    editor = LogFileEditorProvider().createEditor(project, file) as LogFileEditor
    // The platform creates a TrafficLightRenderer asynchronously for a visible error stripe. It can outlive the test and fail the setUp of
    // the next one. The test checks only the highlighters of the markup model, so the stripe can stay hidden.
    (editor.editor.markupModel as EditorMarkupModel).isErrorStripeVisible = false
    val mapRenderer = LogFileMapRenderer.getLogFileMapRenderer(editor.editor)
    assertNotNull(mapRenderer)
    PlatformTestUtil.waitWithEventsDispatching("The event map was not computed", { mapRenderer!!.isEventMapComputed() }, 10)
    NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
    UIUtil.dispatchAllInvocationEvents()
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    val markupModel = editor.editor.markupModel
    assertEquals(1024, markupModel.allHighlighters.size)
    return markupModel
  }

  private fun Color.toTimeIndependent(): Color = Color(abs(this.red - this.blue), 0, 0)
}
