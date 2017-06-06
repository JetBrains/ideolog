package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.openapi.components.ApplicationComponent

class LogIntentionLoaderComponent: ApplicationComponent {
  override fun getComponentName(): String {
    return javaClass.name
  }

  override fun initComponent() {
    IntentionManager.getInstance()
  }

  override fun disposeComponent() {
  }
}
