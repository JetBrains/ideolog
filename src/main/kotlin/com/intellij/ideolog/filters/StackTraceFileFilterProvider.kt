package com.intellij.ideolog.filters

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class StackTraceFileFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<Filter> {
    return arrayOf(StackTraceFileFilter(project, LocalFileSystem.getInstance()))
  }
}
