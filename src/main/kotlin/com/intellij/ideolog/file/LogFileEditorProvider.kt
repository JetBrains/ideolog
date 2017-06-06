package com.intellij.ideolog.file

import com.intellij.ideolog.fileType.LogFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class LogFileEditorProvider : FileEditorProvider {
    val myProvider = TextEditorProvider()

    override fun getEditorTypeId() = "LogFileEditorProvider"

    override fun accept(project: Project, file: VirtualFile) = TextEditorProvider.isTextFile(file) && file.fileType.name == LogFileType.name

    override fun createEditor(project: Project, file: VirtualFile) = LogFileEditor(myProvider.createEditor(project, file) as TextEditorImpl)

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }

    override fun disposeEditor(editor: FileEditor) {
        myProvider.disposeEditor(editor)
    }

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
        return myProvider.readState(sourceElement, project, file)
    }

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
        myProvider.writeState(state, project, targetElement)
    }
}
