package com.intellij.ideolog.intentions

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor

class LogHideThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return IdeologBundle.message("intention.name.hide.lines.with.in.this.field", fieldText)
  }

  override fun getIntentionItems(editor: Editor): HashSet<Pair<Int, String>> =
    editor.document.ideologContext.hiddenItems
}
