package com.intellij.ideolog.highlighting.settings

import com.intellij.ide.BrowserUtil
import com.intellij.ideolog.IdeologBundle
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColorPanel
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBIntSpinner
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import javax.swing.*

class LogHighlightingPatternSettingsDialog(
  private val item: LogHighlightingPattern,
  private val parsingPatterns: List<Pair<UUID, String>>
) :
  DialogWrapper(null, true, IdeModalityType.IDE) {
  private var myPatternText: EditorTextField? = null
  private var myActionCombo: JComboBox<LogHighlightingAction>? = null
  private var myFormatCombo: JComboBox<Pair<UUID?, String>>? = null
  private var myCaptureGroupId: JBIntSpinner? = null
  private var myForegroundCheck: JCheckBox? = null
  private var myBackgroundCheck: JCheckBox? = null
  private var myForegroundColor: ColorPanel? = null
  private var myBackgroundColor: ColorPanel? = null
  private var myBoldCheck: JCheckBox? = null
  private var myItalicCheck: JCheckBox? = null
  private var myStripeCheck: JCheckBox? = null

  init {
    init()
  }

  override fun getHelpId() = ""

  override fun doHelpAction() {
    BrowserUtil.browse("https://github.com/JetBrains/ideolog/wiki/Highlighting-Patterns")
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel(GridBagLayout())
    val constraints = GridBagConstraints().apply {
      gridx = 0
      gridy = 0
      fill = GridBagConstraints.HORIZONTAL
      anchor = GridBagConstraints.SOUTHEAST
    }

    panel.add(JLabel(IdeologBundle.message("label.pattern")), constraints)

    val patternText = EditorTextField(item.pattern)
    myPatternText = patternText
    constraints.gridx = 1
    panel.add(patternText, constraints)

    constraints.gridy = 1
    val actionSelection = ComboBox(arrayOf(LogHighlightingAction.HIGHLIGHT_MATCH, LogHighlightingAction.HIGHLIGHT_FIELD, LogHighlightingAction.HIGHLIGHT_LINE))
    actionSelection.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        text = (value as LogHighlightingAction).printableName()
        return this
      }
    }
    actionSelection.selectedItem = item.action
    myActionCombo = actionSelection
    panel.add(actionSelection, constraints)
    constraints.gridx = 0
    panel.add(JLabel(IdeologBundle.message("label.action")), constraints)

    constraints.gridx = 0
    constraints.gridy = 2
    panel.add(JLabel(IdeologBundle.message("label.format")), constraints)
    constraints.gridx = 1

    val formatSelection = ComboBox(
      (listOf(Pair(null, "Any")) + parsingPatterns).toTypedArray()
    )
    formatSelection.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        text = if (value is Pair<*, *>) {
          if (value.first is UUID? && value.second is String) {
            (value.second as String)
          } else {
            ""
          }
        } else {
          ""
        }
        return this
      }
    }

    formatSelection.selectedIndex = 0
    for (i in 0 until formatSelection.itemCount) {
      if (formatSelection.getItemAt(i).first == item.formatId) {
        formatSelection.selectedIndex = i
        break
      }
    }

    panel.add(formatSelection, constraints)
    myFormatCombo = formatSelection

    constraints.gridx = 0
    constraints.gridy = 3
    panel.add(JLabel(IdeologBundle.message("capture.group")), constraints)
    val groupSpinner = JBIntSpinner(item.captureGroup + 1, 0, 100)
    myCaptureGroupId = groupSpinner
    constraints.gridx++
    panel.add(groupSpinner, constraints)

    val fgColor = ColorPanel()
    myForegroundColor = fgColor
    fgColor.selectedColor = item.foregroundColor
    fgColor.isEnabled = item.foregroundColor != null

    val bgColor = ColorPanel()
    myBackgroundColor = bgColor
    bgColor.selectedColor = item.backgroundColor
    bgColor.isEnabled = item.backgroundColor != null

    val fgCheck = JCheckBox(IdeologBundle.message("foreground"), fgColor.isEnabled)
    myForegroundCheck = fgCheck
    fgCheck.addChangeListener { fgColor.isEnabled = fgCheck.isSelected }

    val bgCheck = JCheckBox(IdeologBundle.message("background"), bgColor.isEnabled)
    myBackgroundCheck = bgCheck
    bgCheck.addActionListener { bgColor.isEnabled = bgCheck.isSelected }

    val boldCheck = JCheckBox(IdeologBundle.message("bold"), item.bold)
    val italicCheck = JCheckBox(IdeologBundle.message("italic"), item.italic)

    myBoldCheck = boldCheck
    myItalicCheck = italicCheck

    constraints.gridx = 0
    constraints.gridy = 4
    panel.add(boldCheck, constraints)

    constraints.gridx = 1
    panel.add(italicCheck, constraints)

    constraints.gridx = 0
    constraints.gridy = 5
    panel.add(fgCheck, constraints)

    constraints.gridx = 1
    panel.add(fgColor, constraints)

    constraints.gridy = 6
    panel.add(bgColor, constraints)

    constraints.gridx = 0
    panel.add(bgCheck, constraints)

    val stripeCheck = JCheckBox(IdeologBundle.message("show.on.stripe"), item.showOnStripe)
    myStripeCheck = stripeCheck
    constraints.gridy++
    panel.add(stripeCheck, constraints)

    return panel
  }

  override fun doOKAction() {
    if (DefaultSettingsStoreItems.HighlightingPatternsUUIDs.contains(item.uuid)) {
      item.uuid = UUID.randomUUID()
    }
    myPatternText?.let { item.pattern = it.text }
    myActionCombo?.let { item.action = it.selectedItem as LogHighlightingAction }
    myFormatCombo?.let { item.formatId = (it.selectedItem as Pair<*, *>).first as UUID? }
    myCaptureGroupId?.let { item.captureGroup = it.number - 1 }
    myForegroundCheck?.let {
      item.foregroundColor = if (it.isSelected) myForegroundColor?.selectedColor else null
    }
    myBackgroundCheck?.let {
      item.backgroundColor = if (it.isSelected) myBackgroundColor?.selectedColor else null
    }
    myBoldCheck?.let { item.bold = it.isSelected }
    myItalicCheck?.let { item.italic = it.isSelected }
    myStripeCheck?.let { item.showOnStripe = it.isSelected }
    super.doOKAction()
  }
}
