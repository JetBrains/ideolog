package com.intellij.ideolog.highlighting

import com.intellij.execution.filters.CompositeFilter
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.Alarm
import gnu.trove.TIntHashSet

class LogHeavyFilterService(project: Project): Disposable {

  companion object {
    fun getInstance(project: Project): LogHeavyFilterService {
      return project.getService(LogHeavyFilterService::class.java)
    }

    internal val markupHighlightedExceptionsKey = Key.create<TIntHashSet>("Log.ParsedExceptions")
    internal val markupHyperlinkSupportKey = Key.create<EditorHyperlinkSupport>("Log.ExceptionsHyperlinks")
  }

  private val myFilters: List<Filter> = ConsoleFilterProvider.FILTER_PROVIDERS.extensions.flatMap { it.getDefaultFilters(project).asIterable() }
  private val myCompositeFilter = CompositeFilter(project, myFilters)
  private val myAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  fun enqueueHeavyFiltering(editor: Editor, eventOffset: Int, event: CharSequence) {
    val markupModel = editor.markupModel

    val set = markupModel.getUserData(markupHighlightedExceptionsKey)
      ?: TIntHashSet().also { markupModel.putUserData(markupHighlightedExceptionsKey, it) }

    if (set.contains(eventOffset))
      return

    set.add(eventOffset)

    val hyperlinkSupport = markupModel.getUserData(markupHyperlinkSupportKey)
      ?: EditorHyperlinkSupport(editor, editor.project!!).also { markupModel.putUserData(markupHyperlinkSupportKey, it) }

    fun consumeResult(result: Filter.Result?, addOffset: Boolean) {
      result ?: return
      ApplicationManager.getApplication().invokeLater { // todo: consider MergingQueue if this generates too many events
        val extraOffset = if (addOffset) eventOffset else 0
        result.resultItems.forEach {
          val hyperlinkInfo = it.hyperlinkInfo
          if (hyperlinkInfo != null)
            hyperlinkSupport.createHyperlink(it.highlightStartOffset + extraOffset, it.highlightEndOffset + extraOffset, it.highlightAttributes, hyperlinkInfo)
          else
            markupModel.addRangeHighlighter(it.highlightStartOffset + extraOffset, it.highlightEndOffset + extraOffset, it.highlighterLayer, it.highlightAttributes, HighlighterTargetArea.EXACT_RANGE)
        }
      }
    }

    val lines = event.split('\n')

    val subDoc = DocumentImpl(event)

    var offset = 0
    lines.forEach { line ->
      offset += line.length
      consumeResult(myCompositeFilter.applyFilter(line, eventOffset + offset), false)
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
