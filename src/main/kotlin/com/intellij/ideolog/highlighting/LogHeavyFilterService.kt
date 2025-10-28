package com.intellij.ideolog.highlighting

import com.intellij.execution.filters.CompositeFilter
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.ideolog.filters.BlackListFilterClassProvider
import com.intellij.ideolog.filters.PrioritizedFilter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class LogHeavyFilterService(private val project: Project, val cs: CoroutineScope): Disposable {

  companion object {
    fun getInstance(project: Project): LogHeavyFilterService {
      val serviceClass = DynamicLogFilterServiceClassProvider.EP_NAME.extensionList.firstOrNull()?.getFilterServiceClass()
                         ?: LogHeavyFilterService::class.java
      return project.getService(serviceClass)
    }

    val markupHighlightedExceptionsKey: Key<HashSet<Int>> = Key.create<HashSet<Int>>("Log.ParsedExceptions")
    internal val markupHyperlinkSupportKey = Key.create<EditorHyperlinkSupport>("Log.ExceptionsHyperlinks")
  }

  private val blackListedFilterClasses: Array<Class<out Filter>> by lazy {
    BlackListFilterClassProvider.BLACK_LIST_FILTER_PROVIDER_EP_NAME.extensionList.flatMap {
      it.getBlackListFilterClasses(project).asIterable()
    }.toTypedArray()
  }
  private val myFilters: List<Filter> by lazy {
    ConsoleFilterProvider.FILTER_PROVIDERS.extensionList
      .flatMap { it.getDefaultFilters(project).asIterable() }
      .filterNot { blackListedFilterClasses.contains(it::class.java) }
      .sortedBy { if (it is PrioritizedFilter) -1 else 1 }
  }
  private val myCompositeFilter: CompositeFilter by lazy {
    CompositeFilter(project, myFilters)
  }
  private val hyperlinksFlow = MutableSharedFlow<() -> Unit>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  init {
    cs.launch {
      hyperlinksFlow.collectLatest { action ->
        action()
      }
    }
  }

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
      cs.launch(Dispatchers.EDT) { // todo: consider MergingQueue if this generates too many events
        if (editor.isDisposed) return@launch
        if (markupModel.getUserData(markupHighlightedExceptionsKey) !== set) return@launch
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
      consumeResult(myCompositeFilter.applyFilter(line, eventOffset + offset), false)
      offset += 1
    }
    hyperlinksFlow.tryEmit {
      if (myCompositeFilter.shouldRunHeavy())
        lines.forEachIndexed { index, _ ->
          myCompositeFilter.applyHeavyFilter(subDoc, 0, index) {
            consumeResult(it, true)
          }
        }
    }
  }

  override fun dispose() {}
}
