package com.intellij.ideolog.highlighting.settings

import javax.swing.table.AbstractTableModel

class LogFormatTableModel(var state: LogHighlightingSettingsStore.State) : AbstractTableModel() {
  override fun getRowCount(): Int {
    return state.parsingPatterns.size
  }

  override fun getColumnCount(): Int {
    return 5
  }

  override fun getColumnClass(columnIndex: Int): Class<*> {
    return when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      1, 2, 3, 4 -> java.lang.String::class.java
      else -> Any::class.java
    }
  }

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
    return columnIndex == 0
  }

  override fun getColumnName(column: Int): String {
    return when(column) {
      0 -> "Enabled"
      1 -> "Name"
      2 -> "Message pattern"
      3 -> "Message start pattern"
      4 -> "Time format"
      else -> ""
    }
  }

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    aValue ?: return
    when(columnIndex) {
      0 -> state.parsingPatterns[rowIndex].enabled = aValue as Boolean
    }
  }

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
    return when(columnIndex) {
      -1 -> state.parsingPatterns[rowIndex]
      0 -> state.parsingPatterns[rowIndex].enabled
      1 -> state.parsingPatterns[rowIndex].name
      2 -> state.parsingPatterns[rowIndex].pattern
      3 -> state.parsingPatterns[rowIndex].lineStartPattern
      4 -> state.parsingPatterns[rowIndex].timePattern
      else -> null
    }
  }

  fun updateStore(store: LogHighlightingSettingsStore.State) {
    state = store
    fireTableDataChanged()
  }

  fun addNewFormat(name: String) {
    state.parsingPatterns.add(LogParsingPattern(false, name, "", "", "", -1, -1, -1))
    val index = state.parsingPatterns.size - 1
    fireTableRowsInserted(index, index)
  }

  fun removeFormat(index: Int) {
    state.parsingPatterns.removeAt(index)
    fireTableRowsDeleted(index, index)
  }

}
