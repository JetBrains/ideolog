package com.intellij.ideolog.largeFile.highlighting

import com.intellij.ideolog.highlighting.LogHeavyFilterService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class LargeLogHeavyFilterService(project: Project, cs: CoroutineScope) : LogHeavyFilterService(project, cs), Disposable {

  companion object {
    fun getInstance(project: Project): LogHeavyFilterService {
      return project.getService(LargeLogHeavyFilterService::class.java)
    }
  }

  private val listenedDocuments = HashSet<Document>()

  override fun enqueueHeavyFiltering(editor: Editor, eventOffset: Int, event: CharSequence) {
    val markupModel = editor.markupModel
    val document = editor.document
    if (!listenedDocuments.contains(document)) {
      document.addDocumentListener(object : DocumentListener {
        override fun beforeDocumentChange(event: DocumentEvent) {
          markupModel.putUserData(markupHighlightedExceptionsKey, HashSet())
        }
      })
      listenedDocuments.add(document)
    }
    return super.enqueueHeavyFiltering(editor, eventOffset, event)
  }
}
