package com.intellij.ideolog.file

import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl

class LogFileEditor(val myBaseEditor: TextEditorImpl) : TextEditor by myBaseEditor {
    val log = Logger.getInstance(LogFileEditor::class.java)

    init{
        LogFileMapRenderer.GetOrCreateLogFileMapRenderer(this)
    }
}
