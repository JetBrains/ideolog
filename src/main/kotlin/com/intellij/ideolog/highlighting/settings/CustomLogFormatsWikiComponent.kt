package com.intellij.ideolog.highlighting.settings

import com.intellij.ideolog.IdeologBundle
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.UIUtil

private const val WIKI_URL = "https://github.com/JetBrains/ideolog/wiki/Custom-Log-Formats-Directory"

class CustomLogFormatsWikiHyperlinkLabel : HyperlinkLabel() {
  init {
    setTextWithHyperlink(IdeologBundle.message("link.label.browse.list.hyperlink.custom.log.formats.hyperlink"))
    font = font.deriveFont(font.size - 1.0f)
    foreground = UIUtil.getLabelDisabledForeground()
    setHyperlinkTarget(WIKI_URL)
  }
}
