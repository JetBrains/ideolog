package com.intellij.ideolog.highlighting

import com.intellij.execution.filters.CompositeFilter
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.ideolog.filters.StackTraceFileFilter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.Alarm

open class LogHeavyFilterService(project: Project): Disposable {

  companion object {
    fun getInstance(project: Project): LogHeavyFilterService {
      return project.getService(LogHeavyFilterService::class.java)
    }

    val markupHighlightedExceptionsKey = Key.create<HashSet<Int>>("Log.ParsedExceptions")
    internal val markupHyperlinkSupportKey = Key.create<EditorHyperlinkSupport>("Log.ExceptionsHyperlinks")
  }

  private val myFilters: List<Filter> = ConsoleFilterProvider.FILTER_PROVIDERS.extensions
    .flatMap { it.getDefaultFilters(project).asIterable() }
    .sortedBy { if (it is StackTraceFileFilter) -1 else 1 } // basically, we want StackTraceFileFilter to be first
  private val myCompositeFilter = CompositeFilter(project, myFilters)
  private val myAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  open fun enqueueHeavyFiltering(editor: Editor, eventOffset: Int, event: CharSequence) {
    if (editor.isDisposed) return

    val markupModel = editor.markupModel

    val set = markupModel.getUserData(markupHighlightedExceptionsKey)
      ?: HashSet<Int>().also { markupModel.putUserData(markupHighlightedExceptionsKey, it) }

    synchronized(set) {
      if (set.contains(eventOffset))
        return

      set.add(eventOffset)
    }

    val hyperlinkSupport = markupModel.getUserData(markupHyperlinkSupportKey)
      ?: EditorHyperlinkSupport(editor, editor.project!!).also { markupModel.putUserData(markupHyperlinkSupportKey, it) }

    fun consumeResult(result: Filter.Result?, addOffset: Boolean) {
      result ?: return
      if (editor.isDisposed) return
      ApplicationManager.getApplication().invokeLater { // todo: consider MergingQueue if this generates too many events
        if (editor.isDisposed) return@invokeLater
        val extraOffset = if (addOffset) eventOffset else 0
        result.resultItems.forEach {
          val hyperlinkInfo = it.hyperlinkInfo
          val highlightStartOffset = it.highlightStartOffset + extraOffset
          val highlightEndOffset = it.highlightEndOffset + extraOffset
          if (highlightEndOffset > editor.document.textLength) return@forEach
          if (hyperlinkInfo != null)
            hyperlinkSupport.createHyperlink(highlightStartOffset, highlightEndOffset, it.highlightAttributes, hyperlinkInfo)
          else
            markupModel.addRangeHighlighter(highlightStartOffset, highlightEndOffset, it.highlighterLayer, it.highlightAttributes,
                                            HighlighterTargetArea.EXACT_RANGE)
        }
      }
    }

    val lines = event.split('\n')

    val subDoc = DocumentImpl(event)

    var offset = 0
    lines.forEach { line ->
      offset += line.length
      consumeResult(myCompositeFilter.applyFilter(line, offset), true)
      offset += 1
    }
    myAlarm.addRequest({
      if(myCompositeFilter.shouldRunHeavy())
        lines.forEachIndexed { index, _ ->
          myCompositeFilter.applyHeavyFilter(subDoc, 0, index) {
            consumeResult(it, true)
          }
        }
    }, 0)
  }

  override fun dispose() {}
}
