// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ideolog.file

import com.intellij.CommonBundle
import com.intellij.diagnostic.VMOptions
import com.intellij.ide.IdeBundle
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.actions.EditCustomVmOptionsAction
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.LargeFileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.sun.management.VMOption
import java.io.File
import java.io.IOException
import java.util.*

class LogFileNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {
    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("log.large.file.editor.notification")
        private val HIDDEN_KEY = Key.create<String>("log.large.file.editor.notification.hidden")
        private val DISABLE_KEY = "log.large.file.editor.notification.disabled"
        private val APPLY_KEY = "log.large.file.editor.notification.apply"

        private fun update(file: VirtualFile, project: Project) = EditorNotifications.getInstance(project).updateNotifications(file)

        private var dontShowAgain = false
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (fileEditor !is LogFileEditor) return null
        val editor = (fileEditor as TextEditor).editor
        val project = editor.project
        val productName = ApplicationNamesInfo.getInstance().productName.toLowerCase(Locale.US)
        val versionName = ApplicationInfo.getInstance().majorVersion
        val isSupported = productName == "rider" && versionName >= "2018"
        if (project == null || editor.getUserData(HIDDEN_KEY) != null || dontShowAgain || !FileUtilRt.isTooLarge(file.length) || !isSupported) {
            return null
        }

        val panel = EditorNotificationPanel().apply {
            createActionLabel("Increase limits to 1Gb") {
                val vmEditAction = EditCustomVmOptionsAction()
                val e = AnActionEvent.createFromAnAction(vmEditAction, null, "", DataContext.EMPTY_CONTEXT)
                ActionUtil.performActionDumbAware(vmEditAction, e)

                val vmFile = VMOptions.getWriteFile()
                if (vmFile != null && open(project, vmFile)) {
                    VMOptions.writeOption("idea.max.content.load.filesize", "=", "1000000")
                    VMOptions.writeOption("idea.max.content.load.large.preview.size", "=", "1000000")
                    VMOptions.writeOption("idea.max.intellisense.filesize", "=", "1000000")
                    SaveAndSyncHandler.getInstance().refreshOpenFiles()
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
                } else {
                    Messages.showErrorDialog(project, "Can't find custom vm options: create it or specify size 1000000 for system properties\n" +
                        "idea.max.content.load.filesize \nidea.max.content.load.large.preview.size \nidea.max.intellisense.filesize", "No custom vm options specified")
                }

                update(file, project)
            }
            createActionLabel("Hide notification") {
                editor.putUserData(HIDDEN_KEY, "true")
                update(file, project)
            }
            createActionLabel("Don't show again") {
                dontShowAgain = true
                update(file, project)
            }
        }

        return panel.text(String.format(
            "File content is truncated. Please increase limits in custom vm options, patch maximum memory (-Xmx) and restart.",
            StringUtil.formatFileSize(file.length),
            StringUtil.formatFileSize(FileUtilRt.LARGE_FILE_PREVIEW_SIZE.toLong())
        ))
    }

    fun open(project: Project, file: File) : Boolean {
        if (!file.exists()) {
            val confirmation = "File \n''${FileUtil.getLocationRelativeToUserHome(file.path)}''\n does not exist. Create?"
            val result = Messages.showYesNoDialog(project, confirmation, "Custom VM options", Messages.getQuestionIcon())
            if (result == Messages.NO) return false

            try {
                FileUtil.writeToFile(file, "# custom ${ApplicationNamesInfo.getInstance().fullProductName} properties\n\n")
            }
            catch(ex: IOException) {
                Logger.getInstance(javaClass).warn(ex)
                val message = "Cannot create file ''$file'': ${ex.message}"
                Messages.showErrorDialog(project, message, CommonBundle.message("title.error"))
                return false
            }
        }

        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (vFile != null) {
            vFile.refresh(false, false)
            OpenFileDescriptor(project, vFile, vFile.length.toInt()).navigate(true)
            return true
        }
        return false
    }
}
