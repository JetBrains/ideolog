package com.intellij.ideolog.intentions

import com.intellij.ideolog.intentions.base.HideLinesIntention

class HideLinesNotContainingSubstringIntention : HideLinesIntention({ it.whitelistedSubstrings }) {

  override fun getText() = "Show only lines containing '$shortSelection'"

}
