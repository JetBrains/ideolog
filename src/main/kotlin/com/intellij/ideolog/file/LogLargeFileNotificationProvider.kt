// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ideolog.file

import com.intellij.diagnostic.VMOptions
import com.intellij.ideolog.IdeologBundle
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isTooLarge
import com.intellij.openapi.vfs.limits.FileSizeLimit
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.*
import java.util.function.Function
import javax.swing.JComponent

class LogLargeFileNotificationProvider : EditorNotificationProvider {
    companion object {
        private val HIDDEN_KEY = Key.create<String>("log.large.file.editor.notification.hidden")

        private fun update(file: VirtualFile, project: Project) = EditorNotifications.getInstance(project).updateNotifications(file)

        private var dontShowAgain = false
    }

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    return Function {
      if (it !is LogFileEditor) return@Function null
      val editor = (it as TextEditor).editor
      val productName = ApplicationNamesInfo.getInstance().productName.lowercase(Locale.getDefault())
      val versionName = ApplicationInfo.getInstance().majorVersion
      val isSupported = productName == "rider" && versionName >= "2018"
      if (editor.getUserData(HIDDEN_KEY) != null || dontShowAgain || !file.isTooLarge() || !isSupported) {
        return@Function null
      }

      val panel = EditorNotificationPanel().apply {
        createActionLabel(IdeologBundle.message("link.label.increase.limits.to.1gb")) {
          VMOptions.setOption("idea.max.content.load.filesize", "1000000")
          VMOptions.setOption("idea.max.content.load.large.preview.size", "1000000")
          VMOptions.setOption("idea.max.intellisense.filesize", "1000000")

          update(file, project)
        }
        createActionLabel(IdeologBundle.message("link.label.hide.notification")) {
          editor.putUserData(HIDDEN_KEY, "true")
          update(file, project)
        }
        createActionLabel(IdeologBundle.message("link.label.don.t.show.again")) {
          dontShowAgain = true
          update(file, project)
        }
      }

      return@Function panel.text(String.format(
        IdeologBundle.message("label.file.content.truncated.please.increase.limits"),
        StringUtil.formatFileSize(file.length),
        StringUtil.formatFileSize(FileSizeLimit.getPreviewLimit(file.extension).toLong())
      ))
    }
  }
}
