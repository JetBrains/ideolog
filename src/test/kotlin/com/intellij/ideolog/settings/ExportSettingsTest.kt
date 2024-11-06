package com.intellij.ideolog.settings

import com.intellij.ideolog.highlighting.settings.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import org.jdom.Element

class ExportSettingsTest : BasePlatformTestCase() {
  fun testExportParsingPattern() = doTest(parsingPatternSelectionIndices = listOf(0)) { element ->
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.first().uuid.toString(),
      element.getChild("parsingPatterns").children.first().getAttributeValue("uuid")
    )
  }

  fun testExportParsingPatterns() = doTest(parsingPatternSelectionIndices = listOf(0, 2)) { element ->
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.first().uuid.toString(),
      element.getChild("parsingPatterns").children.first().getAttributeValue("uuid")
    )
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns[2].uuid.toString(),
      element.getChild("parsingPatterns").children.last().getAttributeValue("uuid")
    )
  }

  fun testExportHighlightingPattern() = doTest(highlightingPatternSelectionIndices = listOf(0)) { element ->
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.patterns.first().uuid.toString(),
      element.getChild("highlightingPatterns").children.first().getAttributeValue("uuid")
    )
  }

  fun testExportHighlightingPatterns() = doTest(highlightingPatternSelectionIndices = listOf(0, 2)) { element ->
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.patterns.first().uuid.toString(),
      element.getChild("highlightingPatterns").children.first().getAttributeValue("uuid")
    )
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.patterns[2].uuid.toString(),
      element.getChild("highlightingPatterns").children.last().getAttributeValue("uuid")
    )
  }

  fun testExportParsingAndHighlightingPatterns() = doTest(parsingPatternSelectionIndices = listOf(0, 2),
                                                          highlightingPatternSelectionIndices = listOf(0, 2)) { element ->
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.first().uuid.toString(),
      element.getChild("parsingPatterns").children.first().getAttributeValue("uuid")
    )
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns[2].uuid.toString(),
      element.getChild("parsingPatterns").children.last().getAttributeValue("uuid")
    )
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.patterns.first().uuid.toString(),
      element.getChild("highlightingPatterns").children.first().getAttributeValue("uuid")
    )
    assertEquals(
      LogHighlightingSettingsStore.getInstance().myState.patterns[2].uuid.toString(),
      element.getChild("highlightingPatterns").children.last().getAttributeValue("uuid")
    )
  }

  private fun doTest(
    parsingPatternSelectionIndices: List<Int> = emptyList(),
    highlightingPatternSelectionIndices: List<Int> = emptyList(),
    assertionsBlock: (Element) -> Unit,
  ) {
    val logHighlightingConfigurable = LogHighlightingConfigurable()
    val logPatternTableModel = LogPatternTableModel(LogHighlightingSettingsStore.getInstance().myState)
    val logFormatTableModel = LogFormatTableModel(LogHighlightingSettingsStore.getInstance().myState)
    val patternsTable = JBTable(logPatternTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
      getColumn(getColumnName(1)).width = JBUI.scale(50)
      getColumn(getColumnName(2)).cellRenderer = LogPatternActionRenderer()
    }
    val formatsTable = JBTable(logFormatTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
    }
    parsingPatternSelectionIndices.forEach { index -> formatsTable.addRowSelectionInterval(index, index) }
    highlightingPatternSelectionIndices.forEach { index -> patternsTable.addRowSelectionInterval(index, index) }
    val element = logHighlightingConfigurable.serializePatternsAndFormats(patternsTable, formatsTable)

    assertNotNull(element)
    assertionsBlock(element!!)
  }
}
