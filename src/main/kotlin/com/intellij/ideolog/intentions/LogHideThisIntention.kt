package com.intellij.ideolog.intentions

import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Editor

class LogHideThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return "Hide lines with '$fieldText' in this field"
  }

  override fun getIntentionItems(editor: Editor): HashSet<Pair<Int, String>> =
    editor.document.ideologContext.hiddenItems
}
