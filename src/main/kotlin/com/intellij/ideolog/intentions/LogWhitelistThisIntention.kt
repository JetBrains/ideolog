package com.intellij.ideolog.intentions

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor

class LogWhitelistThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return IdeologBundle.message("intention.name.show.only.lines.with.in.this.field", fieldText)
  }

  override fun getIntentionItems(editor: Editor): HashSet<Pair<Int, String>> =
    editor.document.ideologContext.whitelistedItems
}
