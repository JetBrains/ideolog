package com.intellij.ideolog.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.rules.TemporaryFolder

class StackTraceFileFilterTest : BasePlatformTestCase() {

  private lateinit var filter: StackTraceFileFilter
  private val tmpFolder: TemporaryFolder = TemporaryFolder()

  override fun setUp() {
    super.setUp()
    tmpFolder.create()
    filter = StackTraceFileFilter(project, LocalFileSystem.getInstance())
  }

  override fun tearDown() {
    try {
      tmpFolder.delete()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun testNoFileHyperlink() {
    assertNoFileHyperlink("")
    assertNoFileHyperlink("No file hyperlink")
    assertNoFileHyperlink("""/Users\me/Application.php""")
    assertNoFileHyperlink("""C:\Users\me/Application.php""")
  }

  fun testSingleFileHyperlink() {
    assertFileHyperlink("hi /Application.php(35)", 3, 23, "/Application.php", 35, false)
    assertFileHyperlink("/Application.php:35", 0, 19, "/Application.php", 35, false)
    assertFileHyperlink("/Users/me/Application.php(35)", 0, 29, "/Users/me/Application.php", 35, false)
    assertFileHyperlink("/Users/me/Application.php:35", 0, 28, "/Users/me/Application.php", 35, false)
    assertFileHyperlink("""C:\Users\me\Application.php(34)""", 0, 31, """C:\Users\me\Application.php""", 34, false)
    assertFileHyperlink("""C:\Users\me\Application.php:34""", 0, 30, """C:\Users\me\Application.php""", 34, false)
  }

  fun testMultipleLogsFileHyperlinks() {
    assertFileHyperlinks(
      applyFilter("{ #stacktrace: /Users/me/Application.php(35) /Users/me/Kernel.php:42 }"),
      listOf(
        FileLinkInfo(15, 44, "/Users/me/Application.php", 35, false),
        FileLinkInfo(45, 68, "/Users/me/Kernel.php", 42, false)
      )
    )
  }

  fun testApplyFilterToExistingFilePathOnLinuxOrMac() {
    val existingFile = tmpFolder.newFile("Application.php")
    val filePathLength = existingFile.absolutePath.length
    assertFileHyperlink("#0 ${existingFile.absolutePath}(2)", 3, 6 + filePathLength, existingFile.absolutePath, 2, true)
  }

  fun testApplyFilterToMultipleExistingFilePathsOnLinuxOrMac() {
    val firstExistingFile = tmpFolder.newFile("Application.php")
    val firstFilePathLength = firstExistingFile.absolutePath.length
    val secondExistingFile = tmpFolder.newFile("Kernel.php")
    val secondFilePathLength = secondExistingFile.absolutePath.length
    assertFileHyperlinks(
      applyFilter("#0 ${firstExistingFile.absolutePath}(2), ${secondExistingFile.absolutePath}(4)"),
      listOf(
        FileLinkInfo(3, 6 + firstFilePathLength, firstExistingFile.absolutePath, 2, true),
        FileLinkInfo(8 + firstFilePathLength, 11 + firstFilePathLength + secondFilePathLength, secondExistingFile.absolutePath, 4, true)
      )
    )
  }

  private fun applyFilter(line: String) = filter.applyFilter(line, line.length)

  private fun assertNoFileHyperlink(text: String) {
    assertNull(applyFilter(text))
  }

  private fun assertFileHyperlink(
    text: String,
    highlightStartOffset: Int,
    highlightEndOffset: Int,
    filePath: String,
    documentLine: Int,
    isFileExists: Boolean
  ) {
    assertFileHyperlinks(
      applyFilter(text),
      listOf(FileLinkInfo(highlightStartOffset, highlightEndOffset, filePath, documentLine, isFileExists))
    )
  }

  private fun assertFileHyperlinks(result: Filter.Result?, infos: List<FileLinkInfo>) {
    assertNotNull(result)
    result?.let {
      val items = result.resultItems
      assertEquals(infos.size, items.size)
      infos.indices.forEach { assertHyperlink(items[it], infos[it]) }
    }
  }

  private fun assertHyperlink(actualItem: Filter.ResultItem, expectedFileLinkInfo: FileLinkInfo) {
    assertEquals(expectedFileLinkInfo.highlightStartOffset, actualItem.highlightStartOffset)
    assertEquals(expectedFileLinkInfo.highlightEndOffset, actualItem.highlightEndOffset)
    if (expectedFileLinkInfo.isFileExists) {
      assertInstanceOf(actualItem.hyperlinkInfo, StackTraceFileFilter.LinedFileHyperlinkInfo::class.java)
      assertFileLink(expectedFileLinkInfo, actualItem.hyperlinkInfo as StackTraceFileFilter.LinedFileHyperlinkInfo)
    } else {
      assertNull(actualItem.hyperlinkInfo)
    }
  }

  private fun assertFileLink(expected: FileLinkInfo, actual: StackTraceFileFilter.LinedFileHyperlinkInfo) {
    assertEquals(expected.filePath, actual.filePath)
    assertEquals(expected.line, actual.documentLine + 1)
  }

  data class FileLinkInfo(
    val highlightStartOffset: Int,
    val highlightEndOffset: Int,
    val filePath: String,
    val line: Int,
    val isFileExists: Boolean
  )
}
