package com.intellij.ideolog.file

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.openapi.vfs.limits.ExtensionSizeLimitInfo
import com.intellij.openapi.vfs.limits.FileSizeLimit

class LogFileSizeLimit : FileSizeLimit {
  override val acceptableExtensions: List<String> = listOf(LogFileType.defaultExtension)

  @Suppress("DEPRECATION")
  override fun getLimits(): ExtensionSizeLimitInfo = ExtensionSizeLimitInfo(
    content = maxOf(LOG_FILE_SIZE_LIMIT, FileSizeLimit.getDefaultContentLoadLimit()),
    intellijSense = maxOf(LOG_FILE_SIZE_LIMIT, PersistentFSConstants.getMaxIntellisenseFileSize()),
    preview = maxOf(LOG_FILE_SIZE_LIMIT, FileUtilRt.LARGE_FILE_PREVIEW_SIZE),
  )

  companion object {
    private const val LOG_FILE_SIZE_LIMIT: Int = 20 * FileUtilRt.MEGABYTE
  }
}
