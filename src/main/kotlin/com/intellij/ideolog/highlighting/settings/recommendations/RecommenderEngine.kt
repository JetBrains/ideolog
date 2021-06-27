package com.intellij.ideolog.highlighting.settings.recommendations

import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

// TODO: Display suggested list in ui
class RecommenderEngine {
  private val wikiUrl = "https://github.com/JetBrains/ideolog/wiki/Custom-Log-Formats-Directory"

  private val recommenders = listOf(
    PhpStormRecommendations()
  ).filter { it.shouldRecommend() }

  fun getComponent(): JComponent {
    // JLabel(fullHtml().wrapWithHtmlBody().toString())
    return HyperlinkLabel().apply {
      setTextWithHyperlink("Browse the list of  <hyperlink>custom log formats</hyperlink>.")
      font = font.deriveFont(font.size-1.0f)
      foreground = UIUtil.getLabelDisabledForeground()
      setHyperlinkTarget(wikiUrl)
    }
  }

  fun fullHtml(): HtmlBuilder {
    return HtmlBuilder()
      .append(recommendation())
      .append(specificRecommendations())
  }

  private fun recommendation(): HtmlBuilder {
    return HtmlBuilder()
      .append("Browse the ")
      .append(HtmlChunk.Element.link(wikiUrl, "list of custom log formats. "))
  }

  private fun specificRecommendations(): HtmlBuilder {
    if (recommenders.isEmpty()) return HtmlBuilder()

    val technologiesNames = recommenders
      .map { it.productName }
      .customTrallJoiner()
    val links = recommenders
      .flatMap { it.getRecommendations() }
      .map { HtmlChunk.Element.link(it.url, it.name) }
      .map { it.toString() }

    if (links.isEmpty()) return HtmlBuilder()

    val highlightersWord = if (links.size == 1) "log format" else "log formats"

    return HtmlBuilder()
      .append("For example, $technologiesNames users will find ")
      .appendRaw(links.customTrallJoiner())
      .append(" ")
      .append(highlightersWord)
      .append(" useful")
  }

  private fun List<String>.customTrallJoiner(): String {
    if (size == 0) return ""
    if (size == 1) return this[0]
    return StringBuilder().run {
      this@customTrallJoiner.take(this@customTrallJoiner.size-2).forEach {
        this.append(it)
        this.append(" ")
      }
      this.append("and ")
      this.append(this@customTrallJoiner[this@customTrallJoiner.size-1])

      toString()
    }
  }
}
