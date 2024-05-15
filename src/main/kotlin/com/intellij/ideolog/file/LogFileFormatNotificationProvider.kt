// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ideolog.file

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ideolog.highlighting.settings.LogHighlightingConfigurable
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.function.Function
import javax.swing.JComponent

class LogFileFormatNotificationProvider : EditorNotificationProvider, DumbAware {
  companion object {
    private val HIDDEN_KEY = Key.create<Any>("log.file.format.editor.notification.hidden")
    private const val DONT_SHOW_AGAIN_KEY = "log.file.format.editor.notification.disabled"

    fun update(file: VirtualFile, project: Project) = EditorNotifications.getInstance(project).updateNotifications(file)
  }

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    return Function {
      if (it !is LogFileEditor) return@Function null
      val editor = (it as TextEditor).editor

      val propertiesComponent = PropertiesComponent.getInstance()
      if (detectLogFileFormat(editor).myRegexLogParser != null || propertiesComponent.getBoolean(DONT_SHOW_AGAIN_KEY) || editor.getUserData(HIDDEN_KEY) != null)
        return@Function null

      val panel = EditorNotificationPanel().apply {
        createActionLabel("Configure log formats") {
          ShowSettingsUtil.getInstance().editConfigurable(project, LogHighlightingConfigurable())

          update(file, project)
        }
        createActionLabel("Hide this notification") {
          editor.putUserData(HIDDEN_KEY, HIDDEN_KEY)

          update(file, project)
        }
        createActionLabel("Don't show again") {
          propertiesComponent.setValue(DONT_SHOW_AGAIN_KEY, true)

          update(file, project)
        }
      }

      return@Function panel.text("Log format not recognized")
    }
  }
}
