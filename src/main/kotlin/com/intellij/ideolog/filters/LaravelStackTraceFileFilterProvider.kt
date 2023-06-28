package com.intellij.ideolog.filters

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class LaravelStackTraceFileFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<Filter> {
    return arrayOf(LaravelStackTraceFileFilter(project, LocalFileSystem.getInstance()))
  }
}
