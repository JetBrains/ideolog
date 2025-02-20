package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.IdeologBundle
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.EditorTextField
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBIntSpinner
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import net.miginfocom.swing.MigLayout
import org.intellij.lang.regexp.RegExpFileType
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class LogParsingPatternSettingsDialog(private val item: LogParsingPattern) : DialogWrapper(null, true, IdeModalityType.IDE) {
  private var myNameText: EditorTextField? = null
  private var myParsingPatternText: EditorTextField? = null
  private var myLineStartPatternText: EditorTextField? = null
  private var myTimePatternText: EditorTextField? = null

  private var myTimeColumnId: JBIntSpinner? = null
  private var mySeverityColumnId: JBIntSpinner? = null
  private var myCategoryColumnId: JBIntSpinner? = null

  init {
    init()
    initValidation()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel(MigLayout("fill, wrap 2", "[right][fill]"))

    val wikiCustomLogFormats = BorderLayoutPanel().apply {
      add(HyperlinkLabel().apply {
        setHyperlinkText(IdeologBundle.message("link.wiki.custom.log.format"))
        setHyperlinkTarget("https://github.com/JetBrains/ideolog/wiki/Custom-Log-Formats")
      })
    }
    panel.add(wikiCustomLogFormats, "span 2, left")

    panel.add(JLabel(IdeologBundle.message("settings.dialog.label.name")))
    val nameText = EditorTextField(item.name)
    myNameText = nameText
    panel.add(nameText)

    panel.add(JLabel(IdeologBundle.message("settings.dialog.label.message.pattern")))
    val patternText = EditorTextField(item.pattern, ProjectManager.getInstance().defaultProject, RegExpFileType.INSTANCE)
    myParsingPatternText = patternText
    panel.add(patternText)

    panel.add(JLabel(IdeologBundle.message("settings.dialog.label.message.start.pattern")))
    val linePatternText = EditorTextField(item.lineStartPattern, ProjectManager.getInstance().defaultProject, RegExpFileType.INSTANCE)
    myLineStartPatternText = linePatternText
    panel.add(linePatternText)

    panel.add(JLabel(IdeologBundle.message("settings.dialog.label.time.format")))
    val timeFormatText = EditorTextField(item.timePattern)
    myTimePatternText = timeFormatText
    panel.add(timeFormatText)
    panel.add(
      JLabel(IdeologBundle.message("settings.dialog.label.preview")).apply {
        foreground = UIUtil.getLabelDisabledForeground()
      }
    )
    val timeFormatPreviewLabel = JLabel(getDatePreviewText(item.timePattern))
    val queue = MergingUpdateQueue("TimePreview", 500, true, MergingUpdateQueue.ANY_COMPONENT, myDisposable)
    timeFormatText.document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        queue.queue(object : Update("typingTime") {
          override fun run() {
            timeFormatPreviewLabel.text = getDatePreviewText(event.document.text)
          }
        })
      }
    }, myDisposable)
    panel.add(timeFormatPreviewLabel)
    panel.add(JLabel())

    val linkLbl = BorderLayoutPanel().apply {
      addToLeft(HyperlinkLabel().apply {
        setHyperlinkText(IdeologBundle.message("link.label.documentation"))
        setHyperlinkTarget("https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html")
      })
    }
    panel.add(linkLbl)


    panel.add(JLabel(IdeologBundle.message("time.capture.group")))
    val timeSpinner = JBIntSpinner(item.timeColumnId + 1, 0, 100)
    myTimeColumnId = timeSpinner
    panel.add(timeSpinner)

    panel.add(JLabel(IdeologBundle.message("severity.capture.group")))
    val severitySpinner = JBIntSpinner(item.severityColumnId + 1, 0, 100)
    mySeverityColumnId = severitySpinner
    panel.add(severitySpinner)

    panel.add(JLabel(IdeologBundle.message("category.capture.group")))
    val categorySpinner = JBIntSpinner(item.categoryColumnId + 1, 0, 100)
    myCategoryColumnId = categorySpinner
    panel.add(categorySpinner)

    return panel
  }

  private fun getDatePreviewText(format: String): String {
    val parsedFormat = try {
      SimpleDateFormat(format).format(Date(629518620000))
    }
    catch (t: Throwable) { "" }

    return parsedFormat.ifBlank { "-" }
  }

  override fun doOKAction() {
    myNameText?.let { item.name = it.text }
    myParsingPatternText?.let { item.pattern = it.text }
    myLineStartPatternText?.let { item.lineStartPattern = it.text }
    myTimePatternText?.let { item.timePattern = it.text }

    myTimeColumnId?.let { item.timeColumnId = it.number - 1 }
    mySeverityColumnId?.let { item.severityColumnId = it.number - 1 }
    myCategoryColumnId?.let { item.categoryColumnId = it.number - 1 }

    if (DefaultSettingsStoreItems.ParsingPatternsUUIDs.contains(item.uuid)) {
      item.uuid = UUID.randomUUID()
    }

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
