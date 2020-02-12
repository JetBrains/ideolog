package com.intellij.ideolog.intentions

import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor

class LogWhitelistThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return "Show only lines with '$fieldText' in this field"
  }

  override fun getIntentionItems(editor: Editor): HashSet<Pair<Int, String>> =
    editor.document.ideologContext.whitelistedItems
}
