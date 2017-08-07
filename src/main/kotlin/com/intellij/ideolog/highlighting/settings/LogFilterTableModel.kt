package com.intellij.ideolog.highlighting.settings

import javax.swing.table.AbstractTableModel

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

  override fun getColumnName(column: Int): String {
    return "Filter"
  }

  override fun getColumnClass(columnIndex: Int) = java.lang.String::class.java

  fun updateStore(store: LogHighlightingSettingsStore.State) {
    state = store
    fireTableDataChanged()
  }
}
