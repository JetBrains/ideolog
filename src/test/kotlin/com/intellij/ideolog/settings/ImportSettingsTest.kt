package com.intellij.ideolog.settings

import com.intellij.ideolog.highlighting.settings.LogHighlightingPattern
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.highlighting.settings.LogParsingPattern
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path

@TestDataPath("\$CONTENT_ROOT/src/test/resources/settings")
class ImportSettingsTest : BasePlatformTestCase() {
  private lateinit var parsingPatternsBackup: List<LogParsingPattern>
  private lateinit var highlightingPatternsBackup: List<LogHighlightingPattern>

  override fun setUp() {
    super.setUp()
    parsingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.map { it.copy() }
    highlightingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.patterns.map { it.copy() }
  }

  override fun tearDown() {
    try {
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.addAll(parsingPatternsBackup)
      LogHighlightingSettingsStore.getInstance().myState.patterns.clear()
      LogHighlightingSettingsStore.getInstance().myState.patterns.addAll(highlightingPatternsBackup)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  override fun getTestDataPath(): String = "src/test/resources/settings"

  fun testImportParsingPattern() {
    val file = JDOMUtil.load(settingsFilePath)
    val state = XmlSerializer.deserialize(file, LogHighlightingSettingsStore.State::class.java)

    LogHighlightingSettingsStore.getInstance().mergeAnotherState(state)
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
  }

  fun testImportParsingPatterns() = doTest {
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.dropLast(1).last().name)
    assertEquals("TestLogFormat2", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
  }

  fun testImportHighlightingPattern() = doTest {
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  fun testImportHighlightingPatterns() = doTest {
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.dropLast(1).last().uuid)
    assertEquals(UUID.fromString("6bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  fun testImportParsingAndHighlightingPatterns() = doTest {
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.dropLast(1).last().name)
    assertEquals("TestLogFormat2", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.dropLast(1).last().uuid)
    assertEquals(UUID.fromString("6bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  fun testImportExistingParsingPattern() {
    val parsingPatternsSize = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.size
    doTest {
      assertEquals(parsingPatternsSize, LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.size)
    }
  }

  fun testImportExistingHighlightingPattern() = doTest {
    val highlightingPatternsSize = LogHighlightingSettingsStore.getInstance().myState.patterns.size
    doTest {
      assertEquals(highlightingPatternsSize, LogHighlightingSettingsStore.getInstance().myState.patterns.size)
    }
  }

  private fun doTest(assertionsBlock: () -> Unit) {
    val file = JDOMUtil.load(settingsFilePath)
    val state = XmlSerializer.deserialize(file, LogHighlightingSettingsStore.State::class.java)

    LogHighlightingSettingsStore.getInstance().mergeAnotherState(state)
    assertionsBlock()
  }

  private val settingsFilePath: Path
    get() = Path("$testDataPath/${getTestName(false)}.xml")
}
