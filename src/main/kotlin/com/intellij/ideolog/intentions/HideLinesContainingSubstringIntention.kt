package com.intellij.ideolog.intentions

import com.intellij.ideolog.foldings.hiddenSubstringsKey

class HideLinesContainingSubstringIntention : HideLinesIntention(hiddenSubstringsKey) {

  override fun getText() = "Hide lines containing '$shortSelection'"

}
