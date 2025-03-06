package com.intellij.ideolog.highlighting.settings

import com.intellij.openapi.extensions.ExtensionPointName

private val EP_NAME: ExtensionPointName<ExternalPatterns> = ExtensionPointName.create("com.intellij.ideolog.externalPatterns")
private val EXTERNAL_PATTERNS = EP_NAME.extensionList

interface ExternalPatterns {
  val parsingPatterns: List<LogParsingPattern>
  val highlightingPatterns: List<LogHighlightingPattern>
}

object ExternalPatternsStore {
  val parsingPatterns: List<LogParsingPattern> = EXTERNAL_PATTERNS.flatMap { it.parsingPatterns }
  val highlightingPatterns: List<LogHighlightingPattern> = EXTERNAL_PATTERNS.flatMap { it.highlightingPatterns }
}
