package com.intellij.ideolog.highlighting.settings

import com.intellij.openapi.options.BaseConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class LogHighlightingConfigurable : BaseConfigurable() {
  private var myLogHighlightingStore: LogHighlightingSettingsStore.State = LogHighlightingSettingsStore.getInstance().myState.clone()
  private val patternTableModel = LogPatternTableModel(myLogHighlightingStore)
  private val filterTableModel = LogFilterTableModel(myLogHighlightingStore)
  private val formatsTableModel = LogFormatTableModel(myLogHighlightingStore)

  override fun getHelpTopic() = null

  override fun createComponent(): JComponent? {
    val heatmapCheckbox = JCheckBox("Display heat map on error stripe/scrollbar", myLogHighlightingStore.errorStripeMode == "heatmap")
    heatmapCheckbox.addChangeListener {
      myLogHighlightingStore.errorStripeMode = if(heatmapCheckbox.isSelected) "heatmap" else "normal"
    }

    val logSizeSpinner = JBIntSpinner(myLogHighlightingStore.readonlySizeThreshold.toInt(), 0, 1024*1024)
    logSizeSpinner.addChangeListener {
      myLogHighlightingStore.readonlySizeThreshold = logSizeSpinner.value.toString()
    }

    val patternsTable = JBTable(patternTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
      getColumn(getColumnName(1)).width = JBUI.scale(50)
      getColumn(getColumnName(2)).cellRenderer = LogPatternActionRenderer()
    }
    val filtersTable = JBTable(filterTableModel).apply { preferredScrollableViewportSize = JBUI.size(10) }
    val formatsTable = JBTable(formatsTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
    }

    val patternsPanel = javax.swing.JPanel(java.awt.BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder("Patterns")
      add(ToolbarDecorator.createDecorator(patternsTable).setAddAction {
        val result = Messages.showInputDialog("Enter new pattern (regex supported):", "New highlighting pattern", null) ?: return@setAddAction
        patternTableModel.addNewPattern(result)
      }.setRemoveAction {
        val selectedIndex = patternsTable.selectedRow
        if (selectedIndex >= 0)
          patternTableModel.removePattern(selectedIndex)
      }.setEditAction {
        val selectedIndex = patternsTable.selectedRow
        if (selectedIndex >= 0)
          LogHighlightingPatternSettingsDialog(patternTableModel.getValueAt(selectedIndex, 2) as LogHighlightingPattern).show()
      }.createPanel(), BorderLayout.CENTER)
    }

    val filtersPanel = javax.swing.JPanel(java.awt.BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder("Filters")
      add(ToolbarDecorator.createDecorator(filtersTable).setAddAction {
        val string = Messages.showInputDialog("Enter new pattern (exact match)", "New filter pattern", null) ?: return@setAddAction
        filterTableModel.addItem(string)
      }.setRemoveAction {
        val selectedIndex = filtersTable.selectedRow
        if (selectedIndex >= 0)
          filterTableModel.removeItem(selectedIndex)
      }.createPanel(), java.awt.BorderLayout.CENTER)
    }

    val formatsPanel = JPanel(BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder("Log Formats")
      add(ToolbarDecorator.createDecorator(formatsTable).apply {
        setAddAction {
          val result = Messages.showInputDialog("Enter new format name:", "New log format", null) ?: return@setAddAction
          formatsTableModel.addNewFormat(result)
        }
        setRemoveAction {
          val selectedIndex = formatsTable.selectedRow
          if (selectedIndex >= 0)
            formatsTableModel.removeFormat(selectedIndex)
        }
        setEditAction {
          val selectedIndex = formatsTable.selectedRow
          if (selectedIndex >= 0)
            LogParsingPatternSettingsDialog(formatsTableModel.getValueAt(selectedIndex, -1) as LogParsingPattern).show()
        }
      }.createPanel(), BorderLayout.CENTER)
    }

    val topPanel = OnePixelSplitter(false, "Ideolog.Settings.TopProportion", 0.5f)
    topPanel.firstComponent = patternsPanel
    topPanel.secondComponent = filtersPanel

    return com.intellij.util.ui.FormBuilder.createFormBuilder()
      .addComponent(heatmapCheckbox)
      .addLabeledComponent("Allow editing log files smaller than (KB, editing can cause performance issues):", logSizeSpinner)
      .addComponentFillVertically(formatsPanel, 0)
      .addComponentFillVertically(topPanel, 0)
      .panel
  }

  override fun apply() {
    LogHighlightingSettingsStore.getInstance().loadState(myLogHighlightingStore)
  }

  override fun isModified(): Boolean {
    val originalState = LogHighlightingSettingsStore.getInstance()
    return originalState.myState != myLogHighlightingStore
  }

  override fun reset() {
    myLogHighlightingStore = LogHighlightingSettingsStore.getInstance().myState.clone()
    patternTableModel.updateStore(myLogHighlightingStore)
    filterTableModel.updateStore(myLogHighlightingStore)
    formatsTableModel.updateStore(myLogHighlightingStore)
  }

  override fun getDisplayName(): String = "Log Highlighting (ideolog)"
}

