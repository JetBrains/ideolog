package com.intellij.ideolog.intentions

import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.highlighting.settings.DefaultSettingsStoreItems
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import kotlin.test.assertEquals

internal class FileMatchTest {
  private val allPattern = DefaultSettingsStoreItems.All
  private val eventWithoutCategory = LogEvent (
    "2002-01-01 13:00:00	TRACE	something.MigrationRequestOperation, Invoking MigrationOperation for namespaces",
    0,
    LogFileFormat(RegexLogParser(Pattern.compile(allPattern.pattern, Pattern.DOTALL), Pattern.compile(allPattern.lineStartPattern), allPattern, SimpleDateFormat(allPattern.timePattern)))
  )
  private val fileMatchWithoutCategory = FileMatch(eventWithoutCategory)

  private val pipeSeparatedPattern = DefaultSettingsStoreItems.PipeSeparated
  private val eventWithCategory = LogEvent (
    "00:00:00.000|warning|MigrationRequestOperation|Invoking Migration",
    0,
    LogFileFormat(RegexLogParser(
      Pattern.compile(pipeSeparatedPattern.pattern, Pattern.DOTALL),
      Pattern.compile(pipeSeparatedPattern.lineStartPattern),
      pipeSeparatedPattern, SimpleDateFormat(pipeSeparatedPattern.timePattern))
    )
  )
  private val fileMatchWithCategory = FileMatch(eventWithCategory)

  @Test
  fun `event without category, filename match = 1`() {
    assertEquals(1, fileMatchWithoutCategory.calculateFilenameMatch("MigrationRequestOperation"))
  }

  @Test
  fun `event without category, filename match = 0`() {
    assertEquals(0, fileMatchWithoutCategory.calculateFilenameMatch("Migration_operation"))
  }

  @Test
  fun `event with category, filename match = 2`() {
    assertEquals(2, fileMatchWithCategory.calculateFilenameMatch("MigrationRequestOperation"))
  }

  @Test
  fun `event with category, filename match = 1`() {
    assertEquals(1, fileMatchWithCategory.calculateFilenameMatch("MigrationRequestOperationName"))
  }

  @Test
  fun `event with category, filename match = 0`() {
    assertEquals(0, fileMatchWithCategory.calculateFilenameMatch("MigrationOperation"))
  }
}
