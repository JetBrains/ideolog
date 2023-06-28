package com.intellij.ideolog.filters

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.LazyFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.util.regex.Matcher
import java.util.regex.Pattern

class LaravelStackTraceFileFilter(
  private val project: Project,
  private val localFileSystem: LocalFileSystem
) : Filter {
  companion object {
    private val LINUX_MACOS_FILE_PATTERN =
      Pattern.compile("\\B/[-A-Za-z0-9+$&@#/%?=~_|!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]\\(\\d+\\)")
    private val WINDOWS_FILE_PATTERN =
      Pattern.compile("\\b[A-Z]:\\\\[-A-Za-z0-9+$&@#\\\\%=~_!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]\\(\\d+\\)")
  }

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val textStartOffset = entireLength - line.length
    val items = collectItems(textStartOffset, LINUX_MACOS_FILE_PATTERN.matcher(line)) +
      collectItems(textStartOffset, WINDOWS_FILE_PATTERN.matcher(line))

    return when (items.size) {
      0 -> null
      1 -> Filter.Result(items[0].highlightStartOffset, items[0].highlightEndOffset, items[0].hyperlinkInfo)
      else -> Filter.Result(items)
    }
  }

  private fun collectItems(textStartOffset: Int, matcher: Matcher): List<Filter.ResultItem> {
    val resultItems = mutableListOf<Filter.ResultItem>()
    while (matcher.find()) {
      resultItems.add(Filter.ResultItem(
        textStartOffset + matcher.start(),
        textStartOffset + matcher.end(),
        buildFileHyperlinkInfo(matcher.group()))
      )
    }
    return resultItems
  }

  private fun buildFileHyperlinkInfo(fileUri: String): HyperlinkInfo? {
    var documentLine = 0
    val filePathEndIndex = fileUri.lastIndexOf('(')

    val filePath = fileUri.substring(0, filePathEndIndex)
    localFileSystem.findFileByPathIfCached(filePath) ?: return null

    val possibleDocumentLine = StringUtil.parseInt(fileUri.substring(filePathEndIndex + 1, fileUri.lastIndex), Int.MIN_VALUE)
    if (possibleDocumentLine != Int.MIN_VALUE) {
      documentLine = possibleDocumentLine - 1
    }
    return LazyFileHyperlinkInfo(project, filePath, documentLine, 0, false)
  }
}
