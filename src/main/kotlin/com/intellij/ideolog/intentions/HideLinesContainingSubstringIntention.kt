package com.intellij.ideolog.intentions

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.intentions.base.HideLinesIntention

class HideLinesContainingSubstringIntention : HideLinesIntention({ it.hiddenSubstrings }) {

  override fun getText(): String = IdeologBundle.message("intention.name.hide.lines.containing", shortSelection)

}
