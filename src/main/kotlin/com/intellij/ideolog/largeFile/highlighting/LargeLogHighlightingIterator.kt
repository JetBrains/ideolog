package com.intellij.ideolog.largeFile.highlighting

import com.intellij.ideolog.highlighting.LogHighlightingIterator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme

class LargeLogHighlightingIterator(
  startOffset: Int,
  editor: Editor,
  textGetter: () -> CharSequence,
  colorGetter: () -> EditorColorsScheme,
) : LogHighlightingIterator(startOffset, editor, textGetter, colorGetter) {
  override fun tryHighlightStacktrace(event: CharSequence, eventOffset: Int) {
    val project = myEditor.project ?: return
    if (!settingsStore.myState.highlightLinks || !ApplicationManager.getApplication().isDispatchThread)
      return

    ApplicationManager.getApplication().executeOnPooledThread {
      val service = LargeLogHeavyFilterService.getInstance(project)
      ApplicationManager.getApplication().runReadAction {
        service.enqueueHeavyFiltering(myEditor, eventOffset, event)
      }
    }
  }
}
