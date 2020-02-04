package com.intellij.ideolog.intentions

class LogWhitelistThisIntention : LogThisIntentionBase() {
  override fun getText(): String {
    return "Show only lines with '$fieldText' in this field"
  }
}
