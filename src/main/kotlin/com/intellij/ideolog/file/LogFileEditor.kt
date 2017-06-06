package com.intellij.ideolog.file

import com.intellij.diagnostic.Dumpable
import com.intellij.ideolog.highlighting.LogFileMapRenderer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.ui.Queryable

class LogFileEditor(val myBaseEditor: TextEditorImpl) : TextEditor by myBaseEditor {
  val log = Logger.getInstance(LogFileEditor::class.java)

  init {
    LogFileMapRenderer.GetOrCreateLogFileMapRenderer(this)
  }

  override fun getEditor(): Editor {
    val editor = myBaseEditor.editor as EditorImpl
    return object: EditorEx by editor, HighlighterClient by editor, Queryable by editor, Dumpable by editor  {
      override fun getSettings(): EditorSettings {
        val settings = editor.settings
        return object: EditorSettings by settings {
          override fun isUseSoftWraps(): Boolean {
            return false
          }
        }
      }

      override fun getDocument() = editor.document
      override fun getProject() = editor.project
      override fun repaint(startOffset: Int, endOffset: Int) = editor.repaint(startOffset, endOffset)
    }
  }

  override fun getState(level: FileEditorStateLevel): FileEditorState {
    return myBaseEditor.getState(level)
  }
}
