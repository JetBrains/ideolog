package com.intellij.ideolog.intentions

class LogHideThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return "Hide lines with '$fieldText' in this field"
  }
}
