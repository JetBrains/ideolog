package com.intellij.ideolog.intentions

import com.intellij.ideolog.foldings.whitelistedSubstringsKey

class HideLinesNotContainingSubstringIntention : HideLinesIntention(whitelistedSubstringsKey) {

  override fun getText() = "Show only lines containing '$shortSelection'"

}
