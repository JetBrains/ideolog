package com.intellij.ideolog.highlighting.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import org.intellij.lang.annotations.Language
import java.awt.Color

@State(name = "LogHighlightingSettings", storages = arrayOf(Storage(value = "log_highlighting.xml", roamingType = RoamingType.DEFAULT)))
class LogHighlightingSettingsStore : PersistentStateComponent<LogHighlightingSettingsStore.State>, Cloneable {
  companion object {
    fun getInstance() = ServiceManager.getService(LogHighlightingSettingsStore::class.java)!!

    val CURRENT_SETTINGS_VERSION = "4"

    @Language("RegExp")
    val cleanState = State(arrayListOf(
      LogHighlightingPattern(true, "^\\s*e(rror)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color.RED.rgb, null, true, false, true),
      LogHighlightingPattern(true, "^\\s*w(arn(ing)?)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color(0xff, 0xaa, 0).rgb, null, true, false, false),
      LogHighlightingPattern(true, "^\\s*i(nfo)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color(0x3f, 0xbf, 0x3f).rgb, null, false, false, false)
    ), arrayListOf(), arrayListOf(
      LogParsingPattern(true, "Pipe-separated", "^(?s)([^|]*)\\|([^|]*)\\|([^|]*)\\|(.*)$", "HH:mm:ss.SSS", "^\\d", 0, 1, 2, false),
      LogParsingPattern(true, "IntelliJ IDEA", "^([^\\[]+)(\\[[\\s\\d]+])\\s*(\\w*)\\s*-\\s*(\\S*)\\s*-(.+)$", "yyyy-MM-dd HH:mm:ss,SSS", "^\\d", 0, 2, 3, false),
      LogParsingPattern(true, "TeamCity build log", "^\\[([^]]+)](.):\\s*(\\[[^]]+])?(.*)$", "HH:mm:ss", "^\\[", 0, 1, 2, false)
    ), CURRENT_SETTINGS_VERSION, "3", "heatmap", "16", true)

    val settingsUpgraders = mapOf<String, (State) -> State>(
      "-1" to { _ -> cleanState.clone() },
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
      }
    )
  }

  var myState = cleanState.clone()
  private val myListeners = HashSet<LogHighlightingSettingsListener>()

  fun addSettingsListener(disposable: Disposable, listener: LogHighlightingSettingsListener) {
    ApplicationManager.getApplication().assertIsDispatchThread()

    myListeners.add(listener)
    Disposer.register(disposable, Disposable {
      myListeners.remove(listener)
    })
  }

  private fun fireListeners() {
    ApplicationManager.getApplication().assertIsDispatchThread()

    myListeners.forEach { it() }
  }

  override fun getState(): LogHighlightingSettingsStore.State {
    return myState
  }

  override fun loadState(state: LogHighlightingSettingsStore.State) {
    XmlSerializerUtil.copyBean(state, myState)

    while(myState.version < CURRENT_SETTINGS_VERSION) {
      val upgrader = settingsUpgraders[myState.version]
      if(upgrader == null) {
        logger("LogHighlightingSettingsStore").warn("Upgrader for version ${myState.version} not found, performing hard reset of settings")
        myState = cleanState.clone()
      } else {
        myState = upgrader(myState)
      }
    }

    if(myState.lastAddedDefaultFormat.toInt() < cleanState.parsingPatterns.size) {
      myState.parsingPatterns.addAll(cleanState.parsingPatterns.subList(myState.lastAddedDefaultFormat.toInt(), cleanState.parsingPatterns.size))
      myState.lastAddedDefaultFormat = cleanState.parsingPatterns.size.toString()
    }

    fireListeners()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as LogHighlightingSettingsStore

    if (myState != other.myState) return false

    return true
  }

  override fun hashCode(): Int {
    return myState.hashCode()
  }

  data class State(
    @XCollection(style = XCollection.Style.v2)
    @Tag("Patterns")
    val patterns: ArrayList<LogHighlightingPattern>,
    @XCollection(style = XCollection.Style.v2)
    @Tag("hidden")
    val hidden: ArrayList<String>,
    @XCollection(style = XCollection.Style.v2)
    @Tag("parsing")
    val parsingPatterns: ArrayList<LogParsingPattern>,
    @Tag("settingsVersion", textIfEmpty = "0")
    var version: String,
    @Tag("lastAddedDefaultFormat", textIfEmpty = "2")
    var lastAddedDefaultFormat: String,
    @Tag("errorStripeModel", textIfEmpty = "heatmap")
    var errorStripeMode: String,
    @Tag("readonlySizeThreshold", textIfEmpty = "1024")
    var readonlySizeThreshold: String,
    @Tag("highlight_links", textIfEmpty = "true")
    var highlightLinks: Boolean
  ) : Cloneable {
    @Suppress("unused")
    constructor() : this(ArrayList(), ArrayList(), ArrayList(), "-1", "-1", "heatmap", "16", true)

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

@Tag("LogParsingPattern")
data class LogParsingPattern(@Attribute("enabled") var enabled: Boolean, @Attribute("name") var name: String, @Attribute("pattern") var pattern: String,
                             @Attribute("timePattern") var timePattern: String, @Attribute("linePattern") var lineStartPattern: String, @Attribute("timeId") var timeColumnId: Int,
                             @Attribute("severityId") var severityColumnId: Int, @Attribute("categoryId") var categoryColumnId: Int,
                             @Attribute("fullmatch") var regexMatchFullEvent: Boolean): Cloneable {

  @Suppress("unused")
  constructor(): this(true, "", "", "", "", -1, -1, -1, false)

  public override fun clone(): LogParsingPattern {
    return LogParsingPattern(enabled, name, pattern, timePattern, lineStartPattern, timeColumnId, severityColumnId, categoryColumnId, regexMatchFullEvent)
  }
}

@Tag("LogHighlightingPattern")
data class LogHighlightingPattern(@Attribute("enabled") var enabled: Boolean, @Attribute("pattern") var pattern: String, @Attribute("action") var action: LogHighlightingAction,
                                  @Attribute("fg") var fgRgb: Int?, @Attribute("bg") var bgRgb: Int?,
                                  @Attribute("bold") var bold: Boolean, @Attribute("italic") var italic: Boolean,
                                  @Attribute("stripe") var showOnStripe: Boolean) : Cloneable {

  @Suppress("unused")
  constructor() : this(true, "", LogHighlightingAction.HIGHLIGHT_FIELD, null, null, false, false, false)

  var foregroundColor: Color?
    @Transient get() = fgRgb?.let { Color(it) }
    @Transient set(value) {
      fgRgb = value?.rgb
    }

  var backgroundColor: Color?
    @Transient get() = bgRgb?.let { Color(it) }
    @Transient set(value) {
      bgRgb = value?.rgb
    }

  public override fun clone(): LogHighlightingPattern {
    return LogHighlightingPattern(enabled, pattern, action, fgRgb, bgRgb, bold, italic, showOnStripe)
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
