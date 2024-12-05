package com.intellij.ideolog.highlighting

import com.intellij.openapi.extensions.ExtensionPointName

interface DynamicLogFilterServiceClassProvider<T : LogHeavyFilterService> {
  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<DynamicLogFilterServiceClassProvider<*>> =
      ExtensionPointName.create("com.intellij.ideolog.dynamicLogFilterServiceClassProvider")
  }

  fun getFilterServiceClass(): Class<T>
}
