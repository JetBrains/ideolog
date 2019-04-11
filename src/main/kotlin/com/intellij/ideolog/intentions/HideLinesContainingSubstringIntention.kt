package com.intellij.ideolog.intentions

import com.intellij.ideolog.intentions.base.HideLinesIntention

class HideLinesContainingSubstringIntention : HideLinesIntention({ it.hiddenSubstrings }) {

  override fun getText() = "Hide lines containing '$shortSelection'"

}
