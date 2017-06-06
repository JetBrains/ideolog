package com.intellij.ideolog.highlighting.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import java.awt.Color
import java.util.*

@State(name = "LogHighlightingSettings", storages = arrayOf(Storage(value = "log_highlighting.xml", roamingType = RoamingType.DEFAULT)))
class LogHighlightingSettingsStore : PersistentStateComponent<LogHighlightingSettingsStore.State>, Cloneable {
  companion object {
    fun getInstance() = ServiceManager.getService(LogHighlightingSettingsStore::class.java)!!
  }

  var myState = State(arrayListOf(
    LogHighlightingPattern(true, "^\\s*e(rror)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color.RED.rgb, null, true, false, true),
    LogHighlightingPattern(true, "^\\s*w(arning)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color(0xff, 0xaa, 0).rgb, null, true, false, false),
    LogHighlightingPattern(true, "^\\s*i(nfo)?\\s*$", LogHighlightingAction.HIGHLIGHT_LINE, Color(0x3f, 0xbf, 0x3f).rgb, null, false, false, false)
  ), arrayListOf())

  override fun getState(): LogHighlightingSettingsStore.State {
    return myState
  }

  override fun loadState(state: LogHighlightingSettingsStore.State) {
    XmlSerializerUtil.copyBean(state, myState)
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
    @AbstractCollection(surroundWithTag = true)
    @Tag("Patterns")
    val patterns: ArrayList<LogHighlightingPattern>,
    @AbstractCollection(surroundWithTag = true)
    @Tag("hidden")
    val hidden: ArrayList<String>
  ) : Cloneable {
    @Suppress("unused")
    constructor() : this(ArrayList(), ArrayList())

    public override fun clone(): State {
      val result = State(ArrayList(), ArrayList())
      patterns.forEach {
        result.patterns.add(it.clone())
      }
      hidden.forEach {
        result.hidden.add(it)
      }
      return result
    }
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
