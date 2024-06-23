package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.highlighting.settings.recommendations.RecommenderEngine
import com.intellij.ideolog.util.application
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.BaseConfigurable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.toByteArray
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.xmlb.XmlSerializer
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*


class LogHighlightingConfigurable : BaseConfigurable() {
  private val store = LogHighlightingSettingsStore.getInstance()
  private var myLogHighlightingState: LogHighlightingSettingsStore.State = store.myState.clone()
  private val patternTableModel = LogPatternTableModel(myLogHighlightingState)
  private val filterTableModel = LogFilterTableModel(myLogHighlightingState)
  private val formatsTableModel = LogFormatTableModel(myLogHighlightingState)

  private val disposable = Disposer.newDisposable()

  override fun getHelpTopic(): Nothing? = null

  override fun createComponent(): JComponent? {
    val heatmapCheckbox = JCheckBox(
      IdeologBundle.message("display.heat.map.on.error.stripe.scrollbar"),
      myLogHighlightingState.errorStripeMode == "heatmap").apply {
      addChangeListener {
        myLogHighlightingState.errorStripeMode = if (this.isSelected) "heatmap" else "normal"
      }
    }

    val linksCheckbox = JCheckBox(
      IdeologBundle.message("highlight.links.and.code.references.in.logs"),
      myLogHighlightingState.highlightLinks).apply {
      addChangeListener {
        myLogHighlightingState.highlightLinks = this.isSelected
      }
    }

    val logSizeSpinner = JBIntSpinner(
      myLogHighlightingState.readonlySizeThreshold.toInt(), 0, 1024*1024).apply {
      addChangeListener {
        myLogHighlightingState.readonlySizeThreshold = this.value.toString()
      }
    }

    val patternsTable = JBTable(patternTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
      getColumn(getColumnName(1)).width = JBUI.scale(50)
      getColumn(getColumnName(2)).cellRenderer = LogPatternActionRenderer()
    }
    val filtersTable = JBTable(filterTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
    }
    val formatsTable = JBTable(formatsTableModel).apply {
      preferredScrollableViewportSize = JBUI.size(10)
      getColumn(getColumnName(0)).maxWidth = JBUI.Fonts.label().size * 15
    }

    val patternsPanel = JPanel(BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder(IdeologBundle.message("border.title.patterns"))

      val panel = ToolbarDecorator.createDecorator(patternsTable).apply {
        setAddAction {
          val result = Messages.showInputDialog(IdeologBundle.message("dialog.message.enter.new.pattern.regex.supported"),
                                                IdeologBundle.message("dialog.title.new.highlighting.pattern"), null) ?: return@setAddAction
          patternTableModel.addNewPattern(result)
        }
        setRemoveAction {
          val selectedIndex = patternsTable.selectedRow
          if (selectedIndex >= 0) patternTableModel.removePattern(selectedIndex)
        }
        setEditAction {
          val selectedIndex = patternsTable.selectedRow
          if (selectedIndex >= 0) {
            LogHighlightingPatternSettingsDialog(
              patternTableModel.getValueAt(selectedIndex, 2) as LogHighlightingPattern,
              formatsTableModel.getParsingPatterns()
            ).show()
          }
        }
      }.createPanel()

      UIUtil.addBorder(panel, JBUI.Borders.emptyRight(IdeBorderFactory.TITLED_BORDER_INDENT))

      add(panel, BorderLayout.CENTER)
    }

    val filtersPanel = JPanel(BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder(IdeologBundle.message("border.title.filters"))
      val panel = ToolbarDecorator.createDecorator(filtersTable).apply {
        setAddAction {
          val string = Messages.showInputDialog(
            IdeologBundle.message("dialog.message.enter.new.pattern.exact.match"),
            IdeologBundle.message("dialog.title.new.filter.pattern"), null) ?: return@setAddAction
          filterTableModel.addItem(string)
        }
        setRemoveAction {
          val selectedIndex = filtersTable.selectedRow
          if (selectedIndex >= 0)
            filterTableModel.removeItem(selectedIndex)
        }
      }.createPanel()

      UIUtil.addBorder(panel, JBUI.Borders.emptyRight(IdeBorderFactory.TITLED_BORDER_INDENT))

      add(panel, BorderLayout.CENTER)
    }

    val formatsPanel = JPanel(BorderLayout()).apply {
      border = IdeBorderFactory.createTitledBorder(IdeologBundle.message("border.title.log.formats"))
      val panel = ToolbarDecorator.createDecorator(formatsTable).apply {
        setAddAction {
          val result = Messages.showInputDialog(
            IdeologBundle.message("dialog.message.enter.new.format.name"),
            IdeologBundle.message("dialog.title.new.log.format"), null) ?: return@setAddAction
          val (newIndex, newFormat) = formatsTableModel.addNewFormat(result)
          val isOk = LogParsingPatternSettingsDialog(newFormat).showAndGet()

          if (!isOk) {
            formatsTableModel.removeFormat(newIndex)
          }
        }
        setRemoveAction {
          val selectedIndex = formatsTable.selectedRow
          if (selectedIndex >= 0)
            formatsTableModel.removeFormat(selectedIndex)
        }
        setEditAction {
          val selectedIndex = formatsTable.selectedRow
          if (selectedIndex >= 0)
            LogParsingPatternSettingsDialog(
              formatsTableModel.getValueAt(selectedIndex, -1) as LogParsingPattern).show()
        }
      }.createPanel()

      val recsPanel = JPanel(MigLayout("novisualpadding, insets 0")).apply {
        border = JBUI.Borders.emptyTop(7)
        add(RecommenderEngine().getComponent())
      }
      add(panel, BorderLayout.CENTER)
      add(recsPanel, BorderLayout.SOUTH)
    }

    val topPanel = OnePixelSplitter(false, "Ideolog.Settings.TopProportion", 0.5f).apply {
      setResizeEnabled(false)
      firstComponent = BorderLayoutPanel().apply {
        addToCenter(patternsPanel)
        border = JBUI.Borders.emptyRight(10)
      }
      secondComponent = BorderLayoutPanel().apply {
        addToCenter(filtersPanel)
        border = JBUI.Borders.emptyLeft(10)
      }
    }

    var formatsTableRange: IntRange? = null
    var patternsTableRange: IntRange? = null
    var filtersTableRange: IntRange? = null

    val theLabel = JLabel(IdeologBundle.message("select.shift.click.for.many.items.to.export.them")).apply {
      foreground = UIUtil.getLabelDisabledForeground()
      border = JBUI.Borders.emptyLeft(10)
    }
    val resetBtn = JButton(IdeologBundle.message("reset.selection")).apply {
      isVisible = false
    }
    val exportBtn = JButton(IdeologBundle.message("export")).apply {
      isVisible = false

      addPropertyChangeListener("enabled") {
        theLabel.isVisible = it.newValue != true
        resetBtn.isVisible = it.newValue == true
      }

      addComponentListener(object : ComponentAdapter() {
        override fun componentHidden(e: ComponentEvent?) {
          theLabel.isVisible = true
          resetBtn.isVisible = false
        }

        override fun componentShown(e: ComponentEvent?) {
          theLabel.isVisible = false
          resetBtn.isVisible = true
        }
      })

      addActionListener {
        // Export button
        val saver = FileChooserFactory.getInstance().createSaveFileDialog(
          FileSaverDescriptor(IdeologBundle.message("dialog.title.save.xml"), "", "xml"),
          this
        )

        val patterns = patternsTableRange?.map { patternTableModel.getValueAt(it,2) as LogHighlightingPattern }
          ?: emptyList()
        val formats = formatsTableRange?.map { formatsTableModel.getValueAt(it,-1) as LogParsingPattern }
          ?: emptyList()

        val store = LogHighlightingSettingsStore.State().apply {
          this.version = LogHighlightingSettingsStore.CURRENT_SETTINGS_VERSION
          this.patterns.addAll(patterns)
          this.parsingPatterns.addAll(formats)
        }

        val serialized = XmlSerializer.serialize(store)

        val fileWrapper = saver.save(VfsUtil.getUserHomeDir(), "ideologExported.xml")

        application.runWriteAction {
          fileWrapper?.getVirtualFile(true)?.setBinaryContent(serialized.toByteArray())
        }
      }
    }

    val importBtn = JButton(IdeologBundle.message("import")).apply {
      addActionListener {
        val chooser = FileChooserFactory.getInstance().createFileChooser(
          FileChooserDescriptorFactory.createSingleFileDescriptor("xml"), null, this
        )

        val vfArr = chooser.choose(null, VfsUtil.getUserHomeDir())

        if (vfArr.size != 1) {
          println("vf null")
          return@addActionListener
        }

        val vf = JDOMUtil.load(vfArr[0].toNioPath().toFile())

        val theState = XmlSerializer.deserialize(vf, LogHighlightingSettingsStore.State::class.java)

        LogHighlightingSettingsStore.getInstance().mergeAnotherState(theState)
      }
    }

    fun removeSelectionFromTable(table: JTable, range: IntRange) {
      table.selectionModel.removeSelectionInterval(range.first, range.last)
    }

    resetBtn.addActionListener {
      formatsTableRange?.let { it1 -> removeSelectionFromTable(formatsTable, it1) }
      patternsTableRange?.let { it1 -> removeSelectionFromTable(patternsTable, it1) }
      filtersTableRange?.let { it1 -> removeSelectionFromTable(filtersTable, it1) }

      formatsTableRange = null
      patternsTableRange = null
      filtersTableRange = null

      exportBtn.isVisible = false
    }

    fun tableRangeWatcher(table: JTable, setter: (IntRange?) -> Unit) {
      table.selectionModel.addListSelectionListener { e ->
        val lsm = e.source as ListSelectionModel
        val minIndex = lsm.minSelectionIndex
        val maxIndex = lsm.maxSelectionIndex

        if (minIndex == -1) {
          setter(null)
          exportBtn.isVisible = false
        }
        else {
          setter(minIndex..maxIndex)
          exportBtn.isVisible = true
        }
      }
    }

    store.addSettingsListener(disposable) {
      reset()
    }

    tableRangeWatcher(formatsTable) { formatsTableRange = it }
    tableRangeWatcher(patternsTable) { patternsTableRange = it }
    tableRangeWatcher(filtersTable) { filtersTableRange = it }

    return FormBuilder().run {
      addComponent(heatmapCheckbox)
      addComponent(linksCheckbox)
      addLabeledComponent(IdeologBundle.message("label.allow.editing.small.log.files"), logSizeSpinner)
      addComponent(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
        add(importBtn)
        add(exportBtn)
        add(theLabel)
        add(resetBtn)
      })
      addComponentFillVertically(formatsPanel, 0)
      addComponentFillVertically(topPanel, 0)

      panel
    }

  }

  override fun disposeUIResources() {
    Disposer.dispose(disposable)
    super.disposeUIResources()
  }

  override fun apply() {
    LogHighlightingSettingsStore.getInstance().loadState(myLogHighlightingState)
  }

  override fun isModified(): Boolean {
    val originalState = LogHighlightingSettingsStore.getInstance()
    return originalState.myState != myLogHighlightingState
  }

  override fun reset() {
    myLogHighlightingState = store.myState.clone()
    patternTableModel.updateStore(myLogHighlightingState)
    filterTableModel.updateStore(myLogHighlightingState)
    formatsTableModel.updateStore(myLogHighlightingState)
  }

  override fun getDisplayName(): String = IdeologBundle.message("configurable.name.log.highlighting.ideolog")
}

