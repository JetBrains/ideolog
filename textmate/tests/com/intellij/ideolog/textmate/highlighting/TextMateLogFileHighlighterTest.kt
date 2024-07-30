package com.intellij.ideolog.textmate.highlighting

import com.intellij.ideolog.highlighting.LogEditorHighlighter
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TextMateLogFileHighlighterTest: BasePlatformTestCase() {
  fun `test text mate highlighter is selected`() {
    val file = myFixture.addFileToProject("some.log", """
2024-02-12 20:17:02+00:00 [Note] [Entrypoint]: Entrypoint script for MariaDB Server 1:10.11.6+maria~ubu2204 started.
2024-02-12 20:17:02+00:00 [Note] [Entrypoint]: Switching to dedicated user 'mysql'
    """.trimIndent())
    myFixture.openFileInEditor(file.virtualFile)
    val highlighter = myFixture.editor.highlighter
    assertInstanceOf(highlighter, LogEditorHighlighter::class.java)
    assertInstanceOf((highlighter as LogEditorHighlighter).createIterator(0),
                     LexerEditorHighlighter.HighlighterIteratorImpl::class.java)
  }
}
