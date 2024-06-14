package com.intellij.ideolog.largeFile

import com.intellij.largeFilesEditor.editor.LargeFileRegexSearchNotificationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider

class LargeLogFileRegexSearchNotificationProvider(
  private val delegate: LargeFileRegexSearchNotificationProvider = LargeFileRegexSearchNotificationProvider()
) : EditorNotificationProvider {
  override fun collectNotificationData(
    project: Project, file: VirtualFile
  ) = delegate.collectNotificationData(project, file)
}
