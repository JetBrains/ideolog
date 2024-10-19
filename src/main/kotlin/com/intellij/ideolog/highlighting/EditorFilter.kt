package com.intellij.ideolog.highlighting

import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.Editor

interface EditorFilter : Filter {
  fun setEditor(editor: Editor)
}
