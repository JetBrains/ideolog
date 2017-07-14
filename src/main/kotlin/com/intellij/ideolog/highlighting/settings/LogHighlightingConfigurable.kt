package com.intellij.ideolog.highlighting.settings

import com.intellij.openapi.options.BaseConfigurable
import com.intellij.openapi.ui.Messages
import javax.swing.table.AbstractTableModel

class LogHighlightingConfigurable : BaseConfigurable() {
  private var myLogHighlightingStore: LogHighlightingSettingsStore.State = LogHighlightingSettingsStore.getInstance().myState.clone()
  private val patternTableModel = LogPatternTableModel(myLogHighlightingStore)
  private val filterTableModel = LogFilterTableModel(myLogHighlightingStore)

  override fun getHelpTopic() = "ideolog"

  override fun createComponent(): javax.swing.JComponent? {
    val patternsTable = com.intellij.ui.table.JBTable(patternTableModel).apply {
      preferredScrollableViewportSize = com.intellij.util.ui.JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = com.intellij.util.ui.JBUI.Fonts.label().size * 10
      getColumn(getColumnName(1)).width = com.intellij.util.ui.JBUI.scale(50)
      getColumn(getColumnName(2)).cellRenderer = LogPatternActionRenderer()
    }
    val filtersTable = com.intellij.ui.table.JBTable().apply { preferredScrollableViewportSize = com.intellij.util.ui.JBUI.size(10) }

    val patternsPanel = javax.swing.JPanel(java.awt.BorderLayout()).apply {
      border = com.intellij.ui.IdeBorderFactory.createTitledBorder("Patterns")
      add(com.intellij.ui.ToolbarDecorator.createDecorator(patternsTable).setAddAction {
        val result = com.intellij.openapi.ui.Messages.showInputDialog("Enter new pattern (regex supported):", "New highlighting pattern", null) ?: return@setAddAction
        patternTableModel.addNewPattern(result)
      }.setRemoveAction {
        val selectedIndex = patternsTable.selectedRow
        if (selectedIndex >= 0)
          patternTableModel.removePattern(selectedIndex)
      }.setEditAction {
        val selectedIndex = patternsTable.selectedRow
        if (selectedIndex >= 0)
          LogHighlightingPatternSettingsDialog(patternTableModel.getValueAt(selectedIndex, 2) as LogHighlightingPattern).show()
      }.createPanel(), java.awt.BorderLayout.CENTER)
    }

    val filtersPanel = javax.swing.JPanel(java.awt.BorderLayout()).apply {
      border = com.intellij.ui.IdeBorderFactory.createTitledBorder("Filters")
      add(com.intellij.ui.ToolbarDecorator.createDecorator(filtersTable).setAddAction {
        val string = Messages.showInputDialog("Enter new pattern (exact match)", "New filter pattern", null) ?: return@setAddAction
        filterTableModel.addItem(string)
      }.setRemoveAction {
        val selectedIndex = patternsTable.selectedRow
        if (selectedIndex >= 0)
          filterTableModel.removeItem(selectedIndex)
      }.createPanel(), java.awt.BorderLayout.CENTER)
    }

    return com.intellij.util.ui.FormBuilder.createFormBuilder().addComponentFillVertically(patternsPanel, 0).addComponentFillVertically(filtersPanel, 0).panel
  }

  override fun apply() {
    LogHighlightingSettingsStore.getInstance().loadState(myLogHighlightingStore)
  }

  override fun isModified(): Boolean {
    val originalState = LogHighlightingSettingsStore.getInstance()
    if (myLogHighlightingStore.patterns.size != originalState.myState.patterns.size)
      return true
    myLogHighlightingStore.patterns.forEachIndexed { index, pattern ->
      if (pattern != originalState.myState.patterns[index])
        return true
    }

    if (myLogHighlightingStore.hidden.size != originalState.myState.hidden.size)
      return true
    myLogHighlightingStore.hidden.forEachIndexed { index, pattern ->
      if (pattern != originalState.myState.hidden[index])
        return true
    }

    return false
  }

  override fun reset() {
    myLogHighlightingStore = LogHighlightingSettingsStore.getInstance().myState.clone()
    patternTableModel.updateStore(myLogHighlightingStore)
    filterTableModel.updateStore(myLogHighlightingStore)
  }

  override fun getDisplayName(): String = "Log Highlighting"
}

class LogFilterTableModel(var state: LogHighlightingSettingsStore.State) : AbstractTableModel() {
  override fun getRowCount(): Int {
    return state.hidden.size
  }

  fun addItem(item: String) {
    state.hidden.add(item)
    val stateSize = state.hidden.size - 1
    fireTableRowsInserted(stateSize, stateSize)
  }

  fun removeItem(index: Int) {
    if (index in 0..(state.hidden.size - 1)) {
      state.hidden.removeAt(index)
      fireTableRowsDeleted(index, index)
    }
  }

  override fun getColumnCount() = 1

  override fun getValueAt(rowIndex: Int, columnIndex: Int) = state.hidden[rowIndex]

  override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    state.hidden[rowIndex] = aValue as String
  }

  override fun getColumnClass(columnIndex: Int) = java.lang.String::class.java

  fun updateStore(store: LogHighlightingSettingsStore.State) {
    state = store
    fireTableDataChanged()
  }
}
