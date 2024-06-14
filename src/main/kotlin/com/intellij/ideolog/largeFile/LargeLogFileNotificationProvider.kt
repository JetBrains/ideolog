package com.intellij.ideolog.largeFile

import com.intellij.largeFilesEditor.editor.LargeFileNotificationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider

class LargeLogFileNotificationProvider(
  private val delegate: LargeFileNotificationProvider = LargeFileNotificationProvider()
) : EditorNotificationProvider {
  override fun collectNotificationData(
    project: Project, file: VirtualFile
  ) = delegate.collectNotificationData(project, file)
}
