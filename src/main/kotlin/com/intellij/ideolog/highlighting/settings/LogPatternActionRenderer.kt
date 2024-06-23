package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.IdeologBundle
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTable

class LogPatternActionRenderer : ColoredTableCellRenderer() {
  override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
    value as LogHighlightingPattern
    val defaultStyle = if (selected) SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES else SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES
    var style = 0
    if (value.bold)
      style = style or SimpleTextAttributes.STYLE_BOLD
    if (value.italic)
      style = style or SimpleTextAttributes.STYLE_ITALIC
    append(value.action.printableName(), SimpleTextAttributes(value.backgroundColor ?: defaultStyle.bgColor, value.foregroundColor ?: defaultStyle.fgColor, null, style))
    if (value.showOnStripe)
      append(IdeologBundle.message("plus.stripe"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
