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

open class StackTraceFileFilter(
  private val project: Project,
  private val localFileSystem: LocalFileSystem
) : Filter, DumbAware {
  companion object {
    private const val FILE_LINE_REGEX = """(\(\d+\)|:\d+)"""
    private const val LINUX_MACOS_PATH_REGEX = """(/(?:[\w.-]+/)*\w+\.\w+)"""
    private const val WINDOWS_PATH_REGEX = """\b((?:[a-zA-Z]:\\(?:[^\\/:*?"<>|\r\n]+\\)*)?[^\\/:*?"<>|\r\n]*)"""
    private val LINUX_MACOS_FILE_PATTERN =
      Pattern.compile("""$LINUX_MACOS_PATH_REGEX$FILE_LINE_REGEX""")
    private val WINDOWS_FILE_PATTERN =
      Pattern.compile("""$WINDOWS_PATH_REGEX$FILE_LINE_REGEX""")

    private fun canContainFilePathFromLaravelLogs(line: String): Boolean {
      return (line.contains(":\\") || line.contains('/')) &&
        ((line.contains('(') && line.contains(')')) || line.contains(':'))
    }
  }

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    if (!canContainFilePathFromLaravelLogs(line)) return null

    val textStartOffset = entireLength - line.length
    val bombedCharSequence = StringUtil.newBombedCharSequence(line, 100)
    val filterResultItems =
      filterNestedResults(
        collectFilterResultItems(textStartOffset, LINUX_MACOS_FILE_PATTERN.matcher(bombedCharSequence)) +
          collectFilterResultItems(textStartOffset, WINDOWS_FILE_PATTERN.matcher(bombedCharSequence))
      )//.filter { it.hyperlinkInfo != null }

    return when (filterResultItems.size) {
      0 -> null
      1 -> filterResultItems.first().let {
        Filter.Result(
          it.highlightStartOffset,
          it.highlightEndOffset,
          it.hyperlinkInfo
        )
      }

      else -> Filter.Result(filterResultItems)
    }.apply {
      if (this?.firstHyperlinkInfo == null) {
        this?.nextAction = Filter.NextAction.CONTINUE_FILTERING
      }
    }
  }

  private fun filterNestedResults(results: List<Filter.ResultItem>): List<Filter.ResultItem> =
    results
      .sortedBy { it.highlightStartOffset }
      .fold(mutableListOf<Pair<Int, Filter.ResultItem>>()) { acc, result ->
        if ((acc.lastOrNull()?.first ?: -1) < result.highlightStartOffset) {
          acc.add(result.highlightEndOffset to result)
        }
        acc
      }
      .map { it.second }

  private fun collectFilterResultItems(textStartOffset: Int, matcher: Matcher): List<Filter.ResultItem> {
    val resultItems = mutableListOf<Filter.ResultItem>()
    while (matcher.find()) {
      resultItems.add(
        Filter.ResultItem(
          textStartOffset + matcher.start(),
          textStartOffset + matcher.end(),
          buildFileHyperlinkInfo(matcher.group(1), matcher.takeIf { it.groupCount() >= 2 }?.group(2))
        )
      )
    }
    return resultItems
  }

  private fun buildFileHyperlinkInfo(filePath: String, documentLineString: String?): HyperlinkInfo? =
    localFileSystem.findFileByPath(filePath)?.let {
      val lineNumber = documentLineString
        ?.filter { it.isDigit() }
        ?.takeIf { it.isNotEmpty() }
        ?.toInt()
        ?.let { it - 1 } ?: 0

      LinedFileHyperlinkInfo(project, filePath, lineNumber)
    }

  class LinedFileHyperlinkInfo(
    project: Project,
    val filePath: String,
    val documentLine: Int,
  ) : LazyFileHyperlinkInfo(project, filePath, documentLine, 0, false)
}
