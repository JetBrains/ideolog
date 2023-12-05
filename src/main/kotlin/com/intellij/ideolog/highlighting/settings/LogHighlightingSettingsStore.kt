package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.util.application
import com.intellij.ideolog.util.getService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import org.intellij.lang.annotations.Language
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object DefaultSettingsStoreItems {
  val PipeSeparated = LogParsingPattern(
    true,
    "Pipe-separated",
    "^(?s)([^|]*)\\|([^|]*)\\|([^|]*)\\|(.*)$",
    "HH:mm:ss.SSS",
    "^\\d",
    0,
    1,
    2,
    false,
    UUID.fromString("b5772998-bf1e-4d9d-ab41-da0b86451163")
  )
  val IntelliJIDEA = LogParsingPattern(
    true,
    "IntelliJ IDEA",
    "^([^\\[]+)(\\[[\\s\\d]+])\\s*(\\w*)\\s*-\\s*(\\S*)\\s*-(.+)$",
    "yyyy-MM-dd HH:mm:ss,SSS",
    "^\\d",
    0,
    2,
    3,
    false,
    UUID.fromString("8a0e8992-94cb-4f4c-8be2-42b03609626b")
  )
  val TeamCityBuildLog = LogParsingPattern(
    true,
    "TeamCity build log",
    "^\\[([^]]+)](.):\\s*(\\[[^]]+])?(.*)$",
    "HH:mm:ss",
    "^\\[",
    0,
    1,
    2,
    false,
    UUID.fromString("e9fa2755-8390-42f5-a41e-a909c58c8cf9")
  )
  val LaravelLog = LogParsingPattern(
    true,
    "Laravel",
    "^\\[([\\d\\-: ]*)] [\\s\\S]*?\\.(.*?(?=:)): ([\\s\\S]*?(?=(^\\[[\\d\\-: ]*])|\\Z))\$",
    "yyyy-MM-dd HH:mm:ss",
    "^\\[\\d",
    0,
    1,
    -1,
    true,
    UUID.fromString("9a75fe1c-24f0-4e5d-8359-ce4dbb9c4c33")
  )
  val Logcat = LogParsingPattern(
    true,
    "Logcat",
    "^(.+)(?:\\s+\\d*\\s+\\d*\\s+)(V|D|I|W|E)\\s([^:]+):(.*)\$",
    "MM:dd HH:mm:ss.mmm",
    "^\\d",
    0,
    1,
    2,
    false,
    UUID.fromString("b8fcb4d4-b1b8-4681-90f1-42f7c02aaf67")
  )
  val Loguru = LogParsingPattern(
    true,
    "Loguru",
    "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s)\\|(\\s[A-Z]*\\s*)\\|(\\s.+:.+:\\d+\\s-\\s.*)\$",
    "yyyy-MM-dd HH:mm:ss.SSS",
    "^\\d",
    0,
    1,
    2,
    false,
    UUID.fromString("19dd1738-1dc7-4df6-b437-18e0800b7782")
  )
  private val ParsingPatterns = listOf(PipeSeparated, IntelliJIDEA, TeamCityBuildLog, LaravelLog, Logcat, Loguru)
  val ParsingPatternsUUIDs = ParsingPatterns.map { it.uuid }

  val Error = LogHighlightingPattern(
    true,
    "^\\s*(e(rror)?|severe)\\s*$",
    LogHighlightingAction.HIGHLIGHT_LINE,
    JBColor.RED.rgb,
    null,
    bold = true,
    italic = false,
    showOnStripe = true,
    uuid = UUID.fromString("de2d3bb2-78c9-4beb-835e-d483c35c07b6")
  )
  val Warning = LogHighlightingPattern(
    true,
    "^\\s*w(arn(ing)?)?\\s*$",
    LogHighlightingAction.HIGHLIGHT_LINE,
    JBColor.ORANGE.rgb,
    null,
    bold = true,
    italic = false,
    showOnStripe = false,
    uuid = UUID.fromString("11ff1574-2118-4722-905a-61bec89b079e")
  )
  val Info = LogHighlightingPattern(
    true,
    "^\\s*i(nfo)?\\s*$",
    LogHighlightingAction.HIGHLIGHT_LINE,
    JBColor.GREEN.rgb,
    null,
    bold = false,
    italic = false,
    showOnStripe = false,
    uuid = UUID.fromString("5e882ebc-2179-488b-8e1a-2fe488636f36")
  )
  private val HighlightingPatterns = listOf(Error, Warning, Info)
  val HighlightingPatternsUUIDs = HighlightingPatterns.map { it.uuid }
}

@State(name = "LogHighlightingSettings", storages = [Storage(value = "log_highlighting.xml", roamingType = RoamingType.DEFAULT)])
class LogHighlightingSettingsStore : PersistentStateComponent<LogHighlightingSettingsStore.State>, Cloneable {
  companion object {
    fun getInstance() = getService<LogHighlightingSettingsStore>()
    val logger = Logger.getInstance("LogHighlightingSettingsStore")

    const val CURRENT_SETTINGS_VERSION = "7"

    val cleanState = State()

    val settingsUpgraders = mapOf<String, (State) -> State>(
      "-1" to { cleanState.clone() },
      "0" to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.version = "1"
        newState.parsingPatterns.addAll(cleanState.parsingPatterns)
        return@lambda newState
      },
      "1" to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.errorStripeMode = "heatmap"
        newState.lastAddedDefaultFormat = "3"
        newState.version = "2"
        return@lambda newState
      },
      "2" to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.version = "3"

        newState.readonlySizeThreshold = "16"
        return@lambda newState
      },
      "3" to lambda@{ oldState ->
        val newState = oldState.clone()
        if (newState.patterns.size >= 3 && newState.patterns[1].pattern == "^\\s*w(arning)?\\s*\$") {
          newState.patterns[1] = newState.patterns[1].copy(pattern = "^\\s*w(arn(ing)?)?\\s*\$")
        }
        newState.version = "4"
        return@lambda newState
      },
      "4" to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.parsingPatterns.forEach {
          when (it.name) {
            DefaultSettingsStoreItems.TeamCityBuildLog.name ->
              it.uuid = DefaultSettingsStoreItems.TeamCityBuildLog.uuid
            DefaultSettingsStoreItems.IntelliJIDEA.name ->
              it.uuid = DefaultSettingsStoreItems.IntelliJIDEA.uuid
            DefaultSettingsStoreItems.PipeSeparated.name ->
              it.uuid = DefaultSettingsStoreItems.PipeSeparated.uuid
            DefaultSettingsStoreItems.LaravelLog.name ->
              it.uuid = DefaultSettingsStoreItems.LaravelLog.uuid
            DefaultSettingsStoreItems.Logcat.name ->
              it.uuid = DefaultSettingsStoreItems.Logcat.uuid
            DefaultSettingsStoreItems.Loguru.name ->
              it.uuid = DefaultSettingsStoreItems.Loguru.uuid
            else ->
              it.uuid = UUID.randomUUID()
          }
        }

        newState.patterns.forEach {
          when (it.pattern) {
            DefaultSettingsStoreItems.Error.pattern ->
              it.uuid = DefaultSettingsStoreItems.Error.uuid
            DefaultSettingsStoreItems.Warning.pattern ->
              it.uuid = DefaultSettingsStoreItems.Warning.uuid
            DefaultSettingsStoreItems.Info.pattern ->
              it.uuid = DefaultSettingsStoreItems.Info.uuid
            else ->
              it.uuid = UUID.randomUUID()
          }
        }

        newState.lastAddedDefaultFormat =
          DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it }

        newState.version = "5"

        return@lambda newState
      },
      "5" to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.lastAddedDefaultFormat =
          DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it }

        newState.version = "6"
        return@lambda newState
      },
      "6" to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.parsingPatterns.removeIf { it.uuid == UUID.fromString("db0779ce-9fd3-11ec-b909-0242ac120002") }
        newState.lastAddedDefaultFormat =
          DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it }

        newState.patterns.find { it.uuid == DefaultSettingsStoreItems.Error.uuid }?.pattern = DefaultSettingsStoreItems.Error.pattern

        newState.version = "7"
        return@lambda newState
      }
    )
  }

  var myState = cleanState.clone()
  private val myListeners = HashSet<LogHighlightingSettingsListener>()

  fun addSettingsListener(disposable: Disposable, listener: LogHighlightingSettingsListener) {
    application.assertIsDispatchThread()

    myListeners.add(listener)
    Disposer.register(disposable) {
      myListeners.remove(listener)
    }
  }

  private fun fireListeners() {
    application.assertIsDispatchThread()

    myListeners.forEach { it() }
  }

  override fun getState(): State {
    return myState
  }

  private fun upgradeState(state: State): State {
    var newState: State = state
    while(newState.version < CURRENT_SETTINGS_VERSION) {
      val upgrader = settingsUpgraders[newState.version]
      newState = if(upgrader == null) {
        logger.warn("Upgrader for version ${newState.version} not found, performing hard reset of settings")
        cleanState.clone()
      } else {
        upgrader(newState)
      }
    }

    return newState
  }

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, myState)
  }

  override fun initializeComponent() {
    myState = upgradeState(myState)

    val lastAddedDefaultFormatOld = try {
      myState.lastAddedDefaultFormat.toInt()
    }
    catch (t: NumberFormatException) {
      return
    }
    if (lastAddedDefaultFormatOld < cleanState.parsingPatterns.size) {
      myState.parsingPatterns.addAll(
        cleanState.parsingPatterns.subList(myState.lastAddedDefaultFormat.toInt(), cleanState.parsingPatterns.size)
      )
      myState.lastAddedDefaultFormat = cleanState.parsingPatterns.size.toString()
      fireListeners()
    }
  }

  /**
   * @return true if has items that were not added
   */
  fun mergeAnotherState(newState: State): Boolean {
    val newParsingPatterns = newState.parsingPatterns.filter { it1 -> myState.parsingPatterns.find { it.uuid == it1.uuid } == null }
    val hasUnimportedItems = newParsingPatterns.size < newState.parsingPatterns.size
    myState.parsingPatterns.addAll(newParsingPatterns)

    fireListeners()

    return hasUnimportedItems
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as LogHighlightingSettingsStore

    return myState == other.myState
  }

  override fun hashCode(): Int {
    return myState.hashCode()
  }

  data class State(
    @XCollection(style = XCollection.Style.v2)
    @Tag("highlightingPatterns")
    val patterns: ArrayList<LogHighlightingPattern>,
    @XCollection(style = XCollection.Style.v2)
    @Tag("hiddenSubstrings")
    val hidden: ArrayList<String>,
    @XCollection(style = XCollection.Style.v2)
    @Tag("parsingPatterns")
    val parsingPatterns: ArrayList<LogParsingPattern>,
    @Tag("settingsVersion")
    var version: String,
    @Tag("lastAddedDefaultFormat")
    var lastAddedDefaultFormat: String,
    @Tag("errorStripeModel")
    var errorStripeMode: String,
    @Tag("readonlySizeThreshold")
    var readonlySizeThreshold: String,
    @Tag("highlight_links")
    var highlightLinks: Boolean
  ) : Cloneable {
    @Suppress("unused")
    constructor() : this(
      arrayListOf(
        DefaultSettingsStoreItems.Error,
        DefaultSettingsStoreItems.Warning,
        DefaultSettingsStoreItems.Info
      ),
      arrayListOf(),
      arrayListOf(
        DefaultSettingsStoreItems.PipeSeparated,
        DefaultSettingsStoreItems.IntelliJIDEA,
        DefaultSettingsStoreItems.TeamCityBuildLog,
        DefaultSettingsStoreItems.LaravelLog,
        DefaultSettingsStoreItems.Loguru,
        DefaultSettingsStoreItems.Logcat,
      ),
      CURRENT_SETTINGS_VERSION,
      DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it },
      "heatmap",
      "16",
      true)

    @Suppress("unused")
    constructor(patterns: ArrayList<LogHighlightingPattern>, hidden: ArrayList<String>, parsingPatterns: ArrayList<LogParsingPattern>) : this(patterns, hidden, parsingPatterns, "-1", "-1", "heatmap", "16", true)

    public override fun clone(): State {
      val result = State(ArrayList(), ArrayList(), ArrayList(), version, lastAddedDefaultFormat, errorStripeMode, readonlySizeThreshold, highlightLinks)
      patterns.forEach {
        result.patterns.add(it.clone())
      }
      hidden.forEach {
        result.hidden.add(it)
      }
      parsingPatterns.forEach {
        result.parsingPatterns.add(it.clone())
      }
      return result
    }
  }
}

class UUIDConverter : Converter<UUID>() {
  override fun toString(value: UUID) = value.toString()
  override fun fromString(value: String): UUID? = UUID.fromString(value)
}

@Tag("LogParsingPattern")
data class LogParsingPattern(@Attribute("enabled") var enabled: Boolean,
                             @Attribute("name") var name: String,
                             @Attribute("pattern") @Language("RegExp") var pattern: String,
                             @Attribute("timePattern") @Language("RegExp") var timePattern: String,
                             @Attribute("linePattern") @Language("RegExp") var lineStartPattern: String,
                             @Attribute("timeId") var timeColumnId: Int,
                             @Attribute("severityId") var severityColumnId: Int,
                             @Attribute("categoryId") var categoryColumnId: Int,
                             @Attribute("fullmatch") var regexMatchFullEvent: Boolean,
                             @Attribute("uuid", converter = UUIDConverter::class) var uuid: UUID): Cloneable {

  @Suppress("unused")
  constructor(): this(true, "", "", "", "", -1, -1, -1, false, UUID.randomUUID())

  public override fun clone(): LogParsingPattern {
    return LogParsingPattern(enabled, name, pattern, timePattern, lineStartPattern, timeColumnId, severityColumnId, categoryColumnId, regexMatchFullEvent, uuid)
  }
}

@Tag("LogHighlightingPattern")
data class LogHighlightingPattern(@Attribute("enabled") var enabled: Boolean,
                                  @Attribute("pattern") @Language("RegExp") var pattern: String,
                                  @Attribute("action") var action: LogHighlightingAction,
                                  @Attribute("fg") var fgRgb: Int?,
                                  @Attribute("bg") var bgRgb: Int?,
                                  @Attribute("bold") var bold: Boolean,
                                  @Attribute("italic") var italic: Boolean,
                                  @Attribute("stripe") var showOnStripe: Boolean,
                                  @Attribute("uuid", converter = UUIDConverter::class) var uuid: UUID) : Cloneable {

  @Suppress("unused")
  constructor() : this(true, "", LogHighlightingAction.HIGHLIGHT_FIELD, null, null, false, false, false, UUID.randomUUID())

  var foregroundColor: Color?
    @Transient get() = fgRgb?.let { JBColor(it, it) }
    @Transient set(value) {
      fgRgb = value?.rgb
    }

  var backgroundColor: Color?
    @Transient get() = bgRgb?.let { JBColor(it, it) }
    @Transient set(value) {
      bgRgb = value?.rgb
    }

  public override fun clone(): LogHighlightingPattern {
    return LogHighlightingPattern(enabled, pattern, action, fgRgb, bgRgb, bold, italic, showOnStripe, uuid)
  }
}

enum class LogHighlightingAction {
  HIGHLIGHT_MATCH,
  HIGHLIGHT_FIELD,
  HIGHLIGHT_LINE,
  HIDE;

  fun printableName() = when (this) {
    HIGHLIGHT_MATCH -> "Highlight match"
    HIGHLIGHT_FIELD -> "Highlight field"
    HIGHLIGHT_LINE -> "Highlight line"
    HIDE -> "Hide"
  }
}
