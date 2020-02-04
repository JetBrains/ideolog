package com.intellij.ideolog.highlighting.settings

import javax.swing.table.AbstractTableModel

class LogPatternTableModel(private var store: LogHighlightingSettingsStore.State) : AbstractTableModel() {
  override fun getRowCount(): Int {
    return store.patterns.size
  }

  override fun getColumnCount(): Int {
    return 3
  }

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
    return columnIndex < 2
  }

  override fun getColumnClass(columnIndex: Int): Class<*> {
    return when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      1 -> java.lang.String::class.java
      2 -> LogHighlightingPattern::class.java
      else -> Any::class.java
    }
  }

  override fun getColumnName(column: Int): String {
    return when (column) {
      0 -> "Enabled"
      1 -> "Pattern"
      2 -> "Action"
      else -> ""
    }
  }

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    val item = store.patterns[rowIndex]
    when (columnIndex) {
      0 -> item.enabled = aValue as Boolean
      1 -> item.pattern = aValue as String
    }
  }

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
    val value = store.patterns[rowIndex]
    return when (columnIndex) {
      0 -> value.enabled
      1 -> value.pattern
      2 -> value
      else -> null
    }
  }

  fun addNewPattern(pattern: String) {
    store.patterns.add(LogHighlightingPattern(true, pattern, LogHighlightingAction.HIGHLIGHT_FIELD, null, null, bold = false, italic = false, showOnStripe = true))
    val index = store.patterns.size - 1
    fireTableRowsInserted(index, index)
  }

  fun removePattern(index: Int) {
    store.patterns.removeAt(index)
    fireTableRowsDeleted(index, index)
  }

  fun updateStore(myLogHighlightingStore: LogHighlightingSettingsStore.State) {
    store = myLogHighlightingStore
    fireTableDataChanged()
  }
}
