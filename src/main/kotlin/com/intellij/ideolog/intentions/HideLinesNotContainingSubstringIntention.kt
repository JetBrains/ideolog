package com.intellij.ideolog.intentions

import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.intentions.base.HideLinesIntention

class HideLinesNotContainingSubstringIntention : HideLinesIntention({ it.whitelistedSubstrings }) {

  override fun getText() = IdeologBundle.message("intention.name.show.only.lines.containing", shortSelection)

}
