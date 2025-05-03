package com.intellij.ideolog.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface BlackListFilterClassProvider {
  companion object {
    internal val BLACK_LIST_FILTER_PROVIDER_EP_NAME: ExtensionPointName<BlackListFilterClassProvider> =
      ExtensionPointName.create("com.intellij.ideolog.blackListFilterClassProvider")
  }

  fun getBlackListFilterClasses(project: Project): Array<Class<out Filter>>
}
