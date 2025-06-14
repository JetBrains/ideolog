package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.util.getService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.JBColor
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import java.awt.Color
import java.util.*

object DefaultSettingsStoreItems {
  val PipeSeparated: LogParsingPattern = LogParsingPattern(
    true,
    "Pipe-separated",
    "^(?s)([^|]*)\\|([^|]*)\\|([^|]*)\\|(.*)$",
    "HH:mm:ss.SSS",
    "^\\d",
    0,
    1,
    2,
    UUID.fromString("b5772998-bf1e-4d9d-ab41-da0b86451163")
  )
  val IntelliJIDEA: LogParsingPattern = LogParsingPattern(
    true,
    "IntelliJ IDEA",
    "^([^\\[]+)(\\[[\\s\\d]+])\\s*(\\w*)\\s*-\\s*(\\S*)\\s*-(.+)$",
    "yyyy-MM-dd HH:mm:ss,SSS",
    "^\\d",
    0,
    2,
    3,
    UUID.fromString("8a0e8992-94cb-4f4c-8be2-42b03609626b")
  )
  val TeamCityBuildLog: LogParsingPattern = LogParsingPattern(
    true,
    "TeamCity build log",
    "^\\[([^]]+)](.):\\s*(\\[[^]]+])?(.*)$",
    "HH:mm:ss",
    "^\\[",
    0,
    1,
    2,
    UUID.fromString("e9fa2755-8390-42f5-a41e-a909c58c8cf9")
  )
  val Logcat: LogParsingPattern = LogParsingPattern(
    true,
    "Logcat",
    "^(.+)(?:\\s+\\d*\\s+\\d*\\s+)(V|D|I|W|E)\\s([^:]+):(.*)\$",
    "MM:dd HH:mm:ss.mmm",
    "^\\d",
    0,
    1,
    2,
    UUID.fromString("b8fcb4d4-b1b8-4681-90f1-42f7c02aaf67")
  )
  val Loguru: LogParsingPattern = LogParsingPattern(
    true,
    "Loguru",
    "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s)\\|(\\s[A-Z]*\\s*)\\|(\\s.+:.+:\\d+\\s-\\s.*)\$",
    "yyyy-MM-dd HH:mm:ss.SSS",
    "^\\d",
    0,
    1,
    2,
    UUID.fromString("19dd1738-1dc7-4df6-b437-18e0800b7782")
  )
  private val ParsingPatterns = listOf(PipeSeparated, IntelliJIDEA, TeamCityBuildLog, Logcat, Loguru)
  internal val ParsingPatternsUUIDs = ParsingPatterns.map { it.uuid }

  val Error: LogHighlightingPattern = LogHighlightingPattern(
    true,
    "^\\s*(e(rror)?|severe)\\s*$",
    null,
    -1,
    LogHighlightingAction.HIGHLIGHT_LINE,
    LogColors.RED,
    null,
    bold = true,
    italic = false,
    showOnStripe = false,
    uuid = UUID.fromString("de2d3bb2-78c9-4beb-835e-d483c35c07b6")
  )
  val Warning: LogHighlightingPattern = LogHighlightingPattern(
    true,
    "^\\s*w(arn(ing)?)?\\s*$",
    null,
    -1,
    LogHighlightingAction.HIGHLIGHT_LINE,
    LogColors.ORANGE,
    null,
    bold = true,
    italic = false,
    showOnStripe = false,
    uuid = UUID.fromString("11ff1574-2118-4722-905a-61bec89b079e")
  )
  val Info: LogHighlightingPattern = LogHighlightingPattern(
    true,
    "^\\s*i(nfo)?\\s*$",
    null,
    -1,
    LogHighlightingAction.HIGHLIGHT_LINE,
    LogColors.GREEN,
    null,
    bold = false,
    italic = false,
    showOnStripe = false,
    uuid = UUID.fromString("5e882ebc-2179-488b-8e1a-2fe488636f36")
  )
  private val HighlightingPatterns = listOf(Error, Warning, Info)
  internal val HighlightingPatternsUUIDs = HighlightingPatterns.map { it.uuid }
}

@State(name = "LogHighlightingSettings", storages = [Storage(value = "log_highlighting.xml", roamingType = RoamingType.DEFAULT)])
class LogHighlightingSettingsStore : PersistentStateComponent<LogHighlightingSettingsStore.State>, Cloneable {
  companion object {
    fun getInstance(): LogHighlightingSettingsStore = getService<LogHighlightingSettingsStore>()
    private val logger = Logger.getInstance("LogHighlightingSettingsStore")

    const val CURRENT_SETTINGS_VERSION: Int = 13

    private val cleanState = State()

    private val settingsUpgraders = mapOf<Int, (State) -> State>(
      -1 to { cleanState.clone() },
      0 to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.version = 1
        newState.parsingPatterns.addAll(cleanState.parsingPatterns)
        return@lambda newState
      },
      1 to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.errorStripeMode = "heatmap"
        newState.lastAddedDefaultFormat = "3"
        newState.version = 2
        return@lambda newState
      },
      2 to lambda@{ oldState ->
        val newState = oldState.clone()
        newState.version = 3

        newState.readonlySizeThreshold = "16"
        return@lambda newState
      },
      3 to lambda@{ oldState ->
        val newState = oldState.clone()
        if (newState.patterns.size >= 3 && newState.patterns[1].pattern == "^\\s*w(arning)?\\s*\$") {
          newState.patterns[1] = newState.patterns[1].copy(pattern = "^\\s*w(arn(ing)?)?\\s*\$")
        }
        newState.version = 4
        return@lambda newState
      },
      4 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.parsingPatterns.forEach {
          when (it.name) {
            DefaultSettingsStoreItems.TeamCityBuildLog.name ->
              it.uuid = DefaultSettingsStoreItems.TeamCityBuildLog.uuid
            DefaultSettingsStoreItems.IntelliJIDEA.name ->
              it.uuid = DefaultSettingsStoreItems.IntelliJIDEA.uuid
            DefaultSettingsStoreItems.PipeSeparated.name ->
              it.uuid = DefaultSettingsStoreItems.PipeSeparated.uuid
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

        newState.version = 5

        return@lambda newState
      },
      5 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.lastAddedDefaultFormat =
          DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it }

        newState.version = 6
        return@lambda newState
      },
      6 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.parsingPatterns.removeIf { it.uuid == UUID.fromString("db0779ce-9fd3-11ec-b909-0242ac120002") }
        newState.lastAddedDefaultFormat =
          DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it }

        newState.patterns.find { it.uuid == DefaultSettingsStoreItems.Error.uuid }?.pattern = DefaultSettingsStoreItems.Error.pattern

        newState.version = 7
        return@lambda newState
      },
      7 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.version = 8
        return@lambda newState
      },
      8 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.version = 9
        return@lambda newState
      },
      9 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.patterns.find { it.uuid == DefaultSettingsStoreItems.Error.uuid }?.pattern = DefaultSettingsStoreItems.Error.pattern

        newState.errorStripeMode = "normal"

        newState.version = 10
        return@lambda newState
      },
      10 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.patterns.find { it.uuid == DefaultSettingsStoreItems.Error.uuid }?.showOnStripe = DefaultSettingsStoreItems.Error.showOnStripe

        newState.version = 11
        return@lambda newState
      },
      11 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.readonlySizeThreshold = (20 * FileUtilRt.MEGABYTE).toString()

        newState.version = 12
        return@lambda newState
      },
      12 to lambda@{ oldState ->
        val newState = oldState.clone()

        newState.parsingPatterns.removeIf { highlightingPattern -> highlightingPattern.uuid in listOf(
          "9a75fe1c-24f0-4e5d-8359-ce4dbb9c4c33",
          "c1c8800e-b27c-4433-8d4a-3ec5a28f72a9"
        ).map(UUID::fromString) }

        newState.version = 13
        return@lambda newState
      },
    )
    private val externalSettingsUpgraders = setOf<(State) -> State> { oldState ->
      val newState = oldState.clone()

      if (!isExternalParamsUpToDate(newState)) {
        newState.externalParsingPatterns = arrayListOf(*ExternalPatternsStore.parsingPatterns.toTypedArray())
        newState.externalHighlightingPatterns = arrayListOf(*ExternalPatternsStore.highlightingPatterns.toTypedArray())
        newState.parsingPatterns.forEach { parsingPattern -> parsingPattern.enabled = false }

        val externalParsingPatternUuids = ExternalPatternsStore.parsingPatterns.map { it.uuid }
        newState.parsingPatterns.removeIf { existingParsingPattern -> existingParsingPattern.uuid in externalParsingPatternUuids }
        newState.parsingPatterns.addAll(ExternalPatternsStore.parsingPatterns)

        val externalHighlightingPatternUuids = ExternalPatternsStore.highlightingPatterns.map { it.uuid }
        newState.patterns.removeIf { existingHighlightingPattern -> existingHighlightingPattern.uuid in externalHighlightingPatternUuids }
        newState.patterns.addAll(ExternalPatternsStore.highlightingPatterns)
      }

      newState.patterns.removeIf { highlightingPattern -> highlightingPattern.uuid in listOf(
        "ceb277ea-e937-431f-93c7-fef9aab016c5",
        "a1f4e368-1ab5-4920-b0cc-6701063bd08e",
        "2dc05512-9066-4439-83b3-fada1287e486",
        "9d83979c-74f1-41cc-b342-7f471e225c1e",
        "f29fc557-7be9-4289-bea3-f4cfb235f23b",
        "1dcadf54-8ba8-431d-9e58-76a10ce12241",
        "18a36367-bd09-4163-8e6d-e2410e6ffd08",
        "acab2e5f-b0b7-4d16-8a88-617c818abfb6",
        "a72b5cbd-5420-47af-91ca-ef8339d2bb82",
        "b4d82f98-2420-47ae-a8d2-a383e7082b85",
      ).map(UUID::fromString) }

      return@setOf newState
    }

    private fun isExternalParamsUpToDate(state: State): Boolean {
      if (state.externalParsingPatterns.size != ExternalPatternsStore.parsingPatterns.size ||
          state.externalHighlightingPatterns.size != ExternalPatternsStore.highlightingPatterns.size
      ) {
        return false
      }

      return state.externalParsingPatterns.toHashSet().containsAll(ExternalPatternsStore.parsingPatterns) &&
             ExternalPatternsStore.parsingPatterns.toHashSet().containsAll(state.externalParsingPatterns) &&
             state.externalHighlightingPatterns.toHashSet().containsAll(ExternalPatternsStore.highlightingPatterns) &&
             ExternalPatternsStore.highlightingPatterns.toHashSet().containsAll(state.externalHighlightingPatterns)
    }
  }

  var myState: LogHighlightingSettingsStore.State = cleanState.clone()
  private val myListeners = HashSet<LogHighlightingSettingsListener>()

  @RequiresEdt
  fun addSettingsListener(disposable: Disposable, listener: LogHighlightingSettingsListener) {
    myListeners.add(listener)
    Disposer.register(disposable) {
      myListeners.remove(listener)
    }
  }

  @RequiresEdt
  private fun fireListeners() {
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
    externalSettingsUpgraders.forEach { externalUpgrader ->
      newState = externalUpgrader(newState)
    }

    return newState
  }

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, myState)
    fireListeners()
  }

  override fun initializeComponent() {
    myState = upgradeState(myState)

    val lastAddedDefaultFormatOld = try {
      myState.lastAddedDefaultFormat.toInt()
    }
    catch (_: NumberFormatException) {
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

  fun mergeAnotherState(newState: State) {
    val newParsingPatterns = newState.parsingPatterns.filter { newParsingPattern ->
      myState.parsingPatterns.find { parsingPattern -> parsingPattern.uuid == newParsingPattern.uuid } == null
    }
    myState.parsingPatterns.addAll(newParsingPatterns)
    val newHighlightingPatterns = newState.patterns.filter { newHighlightingPattern ->
      myState.patterns.find { highlightingPattern -> highlightingPattern.uuid == newHighlightingPattern.uuid } == null
    }
    myState.patterns.addAll(newHighlightingPatterns)

    fireListeners()
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
    val patterns: ArrayList<LogHighlightingPattern> = arrayListOf(
      DefaultSettingsStoreItems.Error,
      DefaultSettingsStoreItems.Warning,
      DefaultSettingsStoreItems.Info
    ),
    @XCollection(style = XCollection.Style.v2)
    @Tag("hiddenSubstrings")
    val hidden: ArrayList<String> = arrayListOf(),
    @XCollection(style = XCollection.Style.v2)
    @Tag("parsingPatterns")
    val parsingPatterns: ArrayList<LogParsingPattern> = arrayListOf(
      DefaultSettingsStoreItems.PipeSeparated,
      DefaultSettingsStoreItems.IntelliJIDEA,
      DefaultSettingsStoreItems.TeamCityBuildLog,
      DefaultSettingsStoreItems.Loguru,
      DefaultSettingsStoreItems.Logcat,
    ),
    @Tag("settingsVersion")
    @Property(alwaysWrite = true)
    var version: Int = 1,
    @Tag("lastAddedDefaultFormat")
    var lastAddedDefaultFormat: String = DefaultSettingsStoreItems.ParsingPatternsUUIDs.map { it.toString() }.joinToString(",") { it },
    @Tag("errorStripeModel")
    var errorStripeMode: String = "normal",
    @Tag("readonlySizeThreshold")
    var readonlySizeThreshold: String = "20480",
    @Tag("highlight_links")
    var highlightLinks: Boolean = true,
    @XCollection(style = XCollection.Style.v2)
    @Tag("externalParsingPatterns")
    var externalParsingPatterns: ArrayList<LogParsingPattern> = arrayListOf(),
    @XCollection(style = XCollection.Style.v2)
    @Tag("externalHighlightingPatterns")
    var externalHighlightingPatterns: ArrayList<LogHighlightingPattern> = arrayListOf(),
  ) : Cloneable {
    public override fun clone(): State {
      val result = State(ArrayList(), ArrayList(), ArrayList(), version, lastAddedDefaultFormat, errorStripeMode, readonlySizeThreshold,
                         highlightLinks, arrayListOf(), arrayListOf())
      patterns.forEach {
        result.patterns.add(it.clone())
      }
      hidden.forEach {
        result.hidden.add(it)
      }
      parsingPatterns.forEach {
        result.parsingPatterns.add(it.clone())
      }
      externalParsingPatterns.forEach {
        result.externalParsingPatterns.add(it.clone())
      }
      externalHighlightingPatterns.forEach {
        result.externalHighlightingPatterns.add(it.clone())
      }
      return result
    }
  }
}

class UUIDConverter : Converter<UUID>() {
  override fun toString(value: UUID): String = value.toString()
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
                             @Attribute("uuid", converter = UUIDConverter::class) var uuid: UUID): Cloneable {

  @Suppress("unused")
  constructor(): this(true, "", "", "", "", -1, -1, -1, UUID.randomUUID())

  public override fun clone(): LogParsingPattern {
    return LogParsingPattern(enabled, name, pattern, timePattern, lineStartPattern, timeColumnId, severityColumnId, categoryColumnId, uuid)
  }
}

@Tag("LogHighlightingPattern")
data class LogHighlightingPattern(@Attribute("enabled") var enabled: Boolean,
                                  @Attribute("pattern") @Language("RegExp") var pattern: String,
                                  @Attribute("formatId", converter = UUIDConverter::class) var formatId: UUID?,
                                  @Attribute("captureGroup") var captureGroup: Int,
                                  @Attribute("action") var action: LogHighlightingAction,
                                  @Attribute("fg", converter = LogColorConverter::class) var fgRgb: LogColor?,
                                  @Attribute("bg", converter = LogColorConverter::class) var bgRgb: LogColor?,
                                  @Attribute("bold") var bold: Boolean,
                                  @Attribute("italic") var italic: Boolean,
                                  @Attribute("stripe") var showOnStripe: Boolean,
                                  @Attribute("uuid", converter = UUIDConverter::class) var uuid: UUID) : Cloneable {

  @Suppress("unused")
  constructor() : this(true, "", null, 0, LogHighlightingAction.HIGHLIGHT_FIELD, null, null, false, false, false, UUID.randomUUID())

  var foregroundColor: Color?
    @Transient get() = fgRgb?.toJBColor()
    @Transient set(value) {
      fgRgb = if (fgRgb == null) LogColor.fromColor(value) else fgRgb?.updateWithColor(value)
    }

  var backgroundColor: Color?
    @Transient get() = bgRgb?.toJBColor()
    @Transient set(value) {
      bgRgb = if (bgRgb == null) LogColor.fromColor(value) else bgRgb?.updateWithColor(value)
    }

  public override fun clone(): LogHighlightingPattern {
    return LogHighlightingPattern(enabled, pattern, formatId, captureGroup, action, fgRgb, bgRgb, bold, italic, showOnStripe, uuid)
  }
}

enum class LogHighlightingAction {
  HIGHLIGHT_MATCH,
  HIGHLIGHT_FIELD,
  HIGHLIGHT_LINE,
  HIDE;

  fun printableName(): String = when (this) {
    HIGHLIGHT_MATCH -> IdeologBundle.message("highlight.match")
    HIGHLIGHT_FIELD -> IdeologBundle.message("highlight.field")
    HIGHLIGHT_LINE -> IdeologBundle.message("highlight.line")
    HIDE -> IdeologBundle.message("hide")
  }
}

/**
 * It was impossible to serialize both light and dark colors of JBColor
 */
@Serializable
data class LogColor(
  val lightRgb: Int,
  val darkRgb: Int,
) {
  companion object {
    fun fromColor(color: Color?): LogColor? {
      return if (color == null) null else LogColor(color.rgb, color.rgb)
    }
  }

  fun toJBColor(): JBColor = JBColor(lightRgb, darkRgb)

  fun updateWithColor(color: Color?): LogColor? {
    if (color == null) {
      return null
    }
    return if (JBColor.isBright()) {
      this.copy(lightRgb = color.rgb)
    }
    else {
      this.copy(darkRgb = color.rgb)
    }
  }
}

private class LogColorConverter : Converter<LogColor>() {
  override fun toString(value: LogColor) = Json.encodeToString(value)
  override fun fromString(value: String): LogColor? = try {
    Json.decodeFromString(value)
  }
  catch (_: SerializationException) {
    val rgb = value.toIntOrNull()
    rgb?.let { LogColor(it, it) }
  }
}
