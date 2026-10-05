package com.intellij.ideolog.settings

import com.intellij.ideolog.highlighting.settings.LogHighlightingPattern
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.highlighting.settings.LogParsingPattern
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.util.xmlb.XmlSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.pathString

@TestApplication
class ImportSettingsTest {
  private lateinit var parsingPatternsBackup: List<LogParsingPattern>
  private lateinit var highlightingPatternsBackup: List<LogHighlightingPattern>
  private lateinit var testName: String

  @BeforeEach
  fun setUp(testInfo: TestInfo) {
    testName = testInfo.testMethod.get().name.removePrefix("test")
    parsingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.map { it.copy() }
    highlightingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.patterns.map { it.copy() }
  }

  @AfterEach
  fun tearDown() {
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.addAll(parsingPatternsBackup)
    LogHighlightingSettingsStore.getInstance().myState.patterns.clear()
    LogHighlightingSettingsStore.getInstance().myState.patterns.addAll(highlightingPatternsBackup)
  }

  private fun getTestDataPath(): String {
    val platformTestDataPath = Path.of(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins/ideolog/src/test/resources/settings")
    if (platformTestDataPath.toFile().exists()) {
      return platformTestDataPath.pathString
    }
    return "src/test/resources/settings"
  }

  @Test
  fun testImportParsingPattern() {
    val file = JDOMUtil.load(settingsFilePath)
    val state = XmlSerializer.deserialize(file, LogHighlightingSettingsStore.State::class.java)

    LogHighlightingSettingsStore.getInstance().mergeAnotherState(state)
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
  }

  @Test
  fun testImportParsingPatterns() = doTest {
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.dropLast(1).last().name)
    assertEquals("TestLogFormat2", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
  }

  @Test
  fun testImportHighlightingPattern() = doTest {
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  @Test
  fun testImportHighlightingPatterns() = doTest {
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.dropLast(1).last().uuid)
    assertEquals(UUID.fromString("6bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  @Test
  fun testImportParsingAndHighlightingPatterns() = doTest {
    assertEquals("TestLogFormat", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.dropLast(1).last().name)
    assertEquals("TestLogFormat2", LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.last().name)
    assertEquals(UUID.fromString("5bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.dropLast(1).last().uuid)
    assertEquals(UUID.fromString("6bcf6a90-78f1-43a4-9a8c-375910f23b12"),
                 LogHighlightingSettingsStore.getInstance().myState.patterns.last().uuid)
  }

  @Test
  fun testImportExistingParsingPattern() {
    val parsingPatternsSize = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.size
    doTest {
      assertEquals(parsingPatternsSize, LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.size)
    }
  }

  @Test
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
    get() = Path("${getTestDataPath()}/$testName.xml")
}
