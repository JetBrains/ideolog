package com.intellij.ideolog.filters

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.LazyFileHyperlinkInfo
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.util.regex.Matcher
import java.util.regex.Pattern

class LaravelStackTraceFileFilter(
  private val project: Project,
  private val localFileSystem: LocalFileSystem
) : Filter, DumbAware {
  companion object {
    private const val FILE_LINE_REGEX = "\\(\\d+\\)"
    private val LINUX_MACOS_FILE_PATTERN =
      Pattern.compile("""\B/(?:[\w.-]+/)*[\w.-]+$FILE_LINE_REGEX""")
    private val WINDOWS_FILE_PATTERN =
      Pattern.compile("""\b[a-zA-Z]:\\(?:[^\\/:*?"<>|\r\n]+\\)*[^\\/:*?"<>|\r\n]*$FILE_LINE_REGEX""")

    private fun canContainFilePathFromLaravelLogs(line: String): Boolean {
      return (line.contains(":\\") || line.contains('/')) && line.contains('(') && line.contains(')')
    }
  }

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    if (!canContainFilePathFromLaravelLogs(line)) return null

    val textStartOffset = entireLength - line.length
    val bombedCharSequence = StringUtil.newBombedCharSequence(line, 100)
    val filterResultItems = collectFilterResultItems(textStartOffset, LINUX_MACOS_FILE_PATTERN.matcher(bombedCharSequence)) +
      collectFilterResultItems(textStartOffset, WINDOWS_FILE_PATTERN.matcher(bombedCharSequence))

    return when (filterResultItems.size) {
      0 -> null
      1 -> Filter.Result(filterResultItems[0].highlightStartOffset, filterResultItems[0].highlightEndOffset, filterResultItems[0].hyperlinkInfo)
      else -> Filter.Result(filterResultItems)
    }
  }

  private fun collectFilterResultItems(textStartOffset: Int, matcher: Matcher): List<Filter.ResultItem> {
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
    val filePathEndIndex = fileUri.lastIndexOf('(')
    val filePath = fileUri.substring(0, filePathEndIndex)
    localFileSystem.findFileByPath(filePath) ?: return null

    val possibleDocumentLine = StringUtil.parseInt(fileUri.substring(filePathEndIndex + 1, fileUri.lastIndex), Int.MIN_VALUE)
    val documentLine = if (possibleDocumentLine != Int.MIN_VALUE) possibleDocumentLine - 1 else 0
    return LinedFileHyperlinkInfo(project, filePath, documentLine)
  }

  class LinedFileHyperlinkInfo(
    project: Project,
    val filePath: String,
    val documentLine: Int,
  ) : LazyFileHyperlinkInfo(project, filePath, documentLine, 0, false)
}
