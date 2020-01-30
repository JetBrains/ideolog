package com.intellij.ideolog.highlighting.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBIntSpinner
import net.miginfocom.swing.MigLayout
import org.intellij.lang.regexp.RegExpFileType
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class LogParsingPatternSettingsDialog(val item: LogParsingPattern) : DialogWrapper(null, true, IdeModalityType.PROJECT) {
  private var myNameText: EditorTextField? = null
  private var myParsingPatternText: EditorTextField? = null
  private var myLineStartPatternText: EditorTextField? = null
  private var myTimePatternText: EditorTextField? = null

  private var myTimeColumnId: JBIntSpinner? = null
  private var mySeverityColumnId: JBIntSpinner? = null
  private var myCategoryColumnId: JBIntSpinner? = null
  private var myOnlyFirstLineRegexCheckbox: JCheckBox? = null

  init {
    init()
    initValidation()
  }

  override fun createCenterPanel(): JComponent? {
    val panel = JPanel(MigLayout("fill, wrap 2", "[right][fill]"))

    panel.add(JLabel("Name: "))
    val nameText = EditorTextField(item.name)
    myNameText = nameText
    panel.add(nameText)

    panel.add(JLabel("Message pattern: "))
    val patternText = EditorTextField(item.pattern, ProjectManager.getInstance().defaultProject, RegExpFileType.INSTANCE)
    myParsingPatternText = patternText
    panel.add(patternText)

    panel.add(JLabel("Message start pattern: "))
    val linePatternText = EditorTextField(item.lineStartPattern, ProjectManager.getInstance().defaultProject, RegExpFileType.INSTANCE)
    myLineStartPatternText = linePatternText
    panel.add(linePatternText)

    panel.add(JLabel("Time format: "))
    val timeFormatText = EditorTextField(item.timePattern)
    myTimePatternText = timeFormatText
    panel.add(timeFormatText)

    panel.add(JLabel("Time capture group: "))
    val timeSpinner = JBIntSpinner(item.timeColumnId + 1, 0, 100)
    myTimeColumnId = timeSpinner
    panel.add(timeSpinner)

    panel.add(JLabel("Severity capture group: "))
    val severitySpinner = JBIntSpinner(item.severityColumnId + 1, 0, 100)
    mySeverityColumnId = severitySpinner
    panel.add(severitySpinner)

    panel.add(JLabel("Category capture group: "))
    val categorySpinner = JBIntSpinner(item.categoryColumnId + 1, 0, 100)
    myCategoryColumnId = categorySpinner
    panel.add(categorySpinner)

    val firstLineRegexCheckbox = JCheckBox("Apply message pattern to all message lines", item.regexMatchFullEvent)
    myOnlyFirstLineRegexCheckbox = firstLineRegexCheckbox
    panel.add(firstLineRegexCheckbox, "span 2, left")

    return panel
  }

  override fun getHelpId() = ""

  override fun doHelpAction() {
    BrowserUtil.browse("https://github.com/JetBrains/ideolog/wiki/Custom-Log-Formats")
  }

  override fun doOKAction() {
    myNameText?.let { item.name = it.text }
    myParsingPatternText?.let { item.pattern = it.text }
    myLineStartPatternText?.let { item.lineStartPattern = it.text }
    myTimePatternText?.let { item.timePattern = it.text }

    myTimeColumnId?.let { item.timeColumnId = it.number - 1 }
    mySeverityColumnId?.let { item.severityColumnId = it.number - 1 }
    myCategoryColumnId?.let { item.categoryColumnId = it.number - 1 }

    myOnlyFirstLineRegexCheckbox?.let { item.regexMatchFullEvent = it.isSelected }

    super.doOKAction()
  }

  override fun doValidateAll(): MutableList<ValidationInfo> {
    val results = ArrayList<ValidationInfo>()

    try {
      myParsingPatternText?.let { Pattern.compile(it.text) }
    } catch(e : PatternSyntaxException) {
      results.add(ValidationInfo(e.localizedMessage, myParsingPatternText))
    }

    try {
      myLineStartPatternText?.let { Pattern.compile(it.text) }
    } catch(e : PatternSyntaxException) {
      results.add(ValidationInfo(e.localizedMessage, myLineStartPatternText))
    }

    try {
      myTimePatternText?.let { SimpleDateFormat(it.text) }
    } catch(e : IllegalArgumentException) {
      results.add(ValidationInfo(e.localizedMessage, myTimePatternText))
    }

    return results
  }
}
