package com.intellij.ideolog.intentions

import com.intellij.ideolog.foldings.whitelistedSubstringsKey

class HideLinesNotContainingSubstringIntention : HideLinesIntention(whitelistedSubstringsKey) {

  override fun getText() = "Hide lines not containing '$shortSelection'"

}
