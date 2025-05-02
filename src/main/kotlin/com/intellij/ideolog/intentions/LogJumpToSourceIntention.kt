package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.find.ngrams.TrigramIndex
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.ManagingFS
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex

internal const val logDelimiters = "\\p{P}|\\s"

private class FileMatch(private val evt: LogEvent) {
  var levelPresent = 0
  var categoryPresent = 0
  var messageTrigramCount = 0

  private var filenameMatch = 0 /* 1 - prefix match to category, 2 - full match to category
                                   in case of not specified category: 1 - raw text of event contains filename */

  private lateinit var _vf: VirtualFile
  var virtualFile: VirtualFile
    get() = _vf
    set(value) {
      _vf = value
      filenameMatch = calculateFilenameMatch(_vf.nameWithoutExtension)
    }

  val priority: Int get() = filenameMatch * 15 + categoryPresent * 3 + levelPresent * 2 + messageTrigramCount

  fun calculateFilenameMatch(name: String): Int {
    if (evt.category.isNotBlank()) {
      if (name == evt.category) return 2
      else if (name.startsWith(evt.category) && /*To suppress short prefixes*/name.length <= evt.category.length * 1.5) {
        return 1
      }
    } else if (evt.rawText.split(Regex(logDelimiters)).contains(name)) {
      return 1
    }
    return 0
  }

  override fun toString(): String {
    return "Match(${virtualFile.presentableName}): (level: $levelPresent, " +
      "category: $categoryPresent, " +
      "message: $messageTrigramCount of ${evt.messageTrigrams.size}, " +
      "priority:  $priority, " +
      "filename: $filenameMatch, " +
      "best: $bestScore" +
      ")"
  }


  var bestScore = 0
  var bestLine = 0


  companion object {
    private const val LIMIT = 100
    val m = Array(LIMIT) { IntArray(LIMIT) }
  }


  fun processFileOffset(psiManager: PsiManager) {
    val text = ReadAction.compute<String, Throwable> {
      psiManager.findFile(virtualFile)?.text
    } ?: return
    val strForSubstring = evt.message.take(LIMIT)

    text.lines().forEachIndexed { line, strInCode ->
      val limitedStrInCode = strInCode.trim().take(LIMIT)

      if (limitedStrInCode.contains("\"")) {
        val score = longestSubstring(m, strForSubstring, limitedStrInCode)
        if (score > bestScore) {
          bestLine = line
          bestScore = score
        }
      }
    }
  }


  private fun longestSubstring(m: Array<IntArray>, a: String, b: String): Int {
    if (a.isEmpty() || b.isEmpty()) {
      return 0
    }

    var maxLength = 0
    for (i in 0..a.lastIndex) {
      for (j in 0..b.lastIndex) {
        if (a[i] == b[j]) {
          if (i != 0 && j != 0) {
            m[i][j] = m[i - 1][j - 1] + 1
          } else {
            m[i][j] = 1
          }
          if (m[i][j] > maxLength) {
            maxLength = m[i][j]
          }
        } else m[i][j] = 0
      }
    }
    return maxLength
  }


}


class LogJumpToSourceIntention : IntentionAction {

  override fun getText(): String {
    return IdeologBundle.message("intention.name.jump.to.source")
  }

  override fun getFamilyName(): String = IdeologBundle.message("intention.family.name.logs")

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    return (file?.fileType == LogFileType)
  }


  override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
    doIt(project, editor)
  }


  companion object {
    private fun getMatchesFromFiles(pfi: ProjectFileIndex, filesWithLevel: MutableSet<VirtualFile>, evt: LogEvent, vfs: Iterable<VirtualFile>, haveCategory: Boolean): List<FileMatch> {
      val matches = ArrayList<FileMatch>()
      vfs.forEach { vf ->
        if (vf.extension == LogFileType.defaultExtension || !pfi.isInSourceContent(vf)) {
          return@forEach
        }

        val match = FileMatch(evt)
        match.virtualFile = vf
        if (haveCategory) {
          match.categoryPresent = 1
        }

        if (filesWithLevel.contains(vf)) {
          match.levelPresent = 1
        }

        matches.add(match)
      }
      return matches
    }

    private fun getFilesToMatch(project: Project, evt: LogEvent): List<FileMatch> {
      val filtered = ArrayList<FileMatch>()

      ApplicationManager.getApplication().runReadAction {
        val cacheManager = CacheManager.getInstance(project)
        val fileIdMap = HashMap<Int, FileMatch>()
        val indexManager = FileBasedIndex.getInstance() as FileBasedIndex
        indexManager.processFilesContainingAllKeys(TrigramIndex.INDEX_ID, evt.messageTrigrams, GlobalSearchScope.projectScope(project), null, Processor {
          val fileId = (it as VirtualFileWithId).id
          var structure = fileIdMap.get(fileId)
          if (structure == null) {
            structure = FileMatch(evt)
            fileIdMap[fileId] = structure
          }
          structure.messageTrigramCount++
          return@Processor true
        })

        val filesWithCategory = cacheManager.getVirtualFilesWithWord(evt.category, UsageSearchContext.ANY, GlobalSearchScope.projectScope(project), false).toMutableSet()
        val filesWithLevel = cacheManager.getVirtualFilesWithWord(evt.level, UsageSearchContext.ANY, GlobalSearchScope.projectScope(project), false).toMutableSet()

        val fs = ManagingFS.getInstance() as PersistentFS
        val pfi = ProjectFileIndex.getInstance(project)


        fileIdMap.entries.forEach { (fileId, match) ->
          val vf = fs.findFileById(fileId)
          if (vf == null
            || !pfi.isInSourceContent(vf)
            || vf.extension !in listOf("kt", "java", "cs", "vb", "cpp")
            ) {
            return@forEach
          }



          match.virtualFile = vf

          if (filesWithCategory.contains(vf)) {
            match.categoryPresent = 1
            filesWithCategory.remove(vf)
          }

          if (filesWithLevel.contains(vf)) {
            match.levelPresent = 1
          }

          filtered.add(match)
        }

        filtered.addAll(getMatchesFromFiles(pfi, filesWithLevel, evt, filesWithCategory, true))

        if (evt.category.isBlank()) {
          /* for events without a category, files for matching can be found using words from the event message,
             but short words are not informative and there may be too many words in the event message,
             so only first 10 words of length at least 5 are considered */
          val filesWithMessagePart = evt.message.split(Regex(logDelimiters)).filter { it.length >= 5 }.take(10).map {
            cacheManager.getVirtualFilesWithWord(it, UsageSearchContext.ANY, GlobalSearchScope.projectScope(project), false).toList()
          }.flatten().distinct()
          filtered.addAll(getMatchesFromFiles(pfi, filesWithLevel, evt, filesWithMessagePart, false))
        }
      }

      filtered.sortBy { -it.priority }

      return filtered
    }

    fun doIt(project: Project, editor: Editor) {
      ProgressManager.getInstance().run(object : Task.Backgroundable(project, IdeologBundle.message("progress.title.finding.source"), false) {
        override fun run(indicator: ProgressIndicator) {
          println("\n<JumpToSource>")

          indicator.text = IdeologBundle.message("progress.text.finding.source")
          indicator.isIndeterminate = true

          val event = ReadAction.compute<LogEvent, Throwable> {
            LogEvent.fromEditor(editor)
          }
          event.prepareTrigrams()
          println(event)


          if (event.message.isBlank()) return

          val sorted = getFilesToMatch(project, event)

          val bestCandidates = sorted.take(3).toMutableList()


          val psiManager = PsiManager.getInstance(project)

          println("Best candidates:")
          bestCandidates.forEach {
            it.processFileOffset(psiManager)
            println(it)
          }



          bestCandidates.sortBy { -it.bestScore }
          val best = bestCandidates.firstOrNull() ?: return


          ApplicationManager.getApplication().invokeLater {
            val descriptor = OpenFileDescriptor(project, best.virtualFile, best.bestLine, 0)
            val navigable = descriptor.setUseCurrentWindow(true)
            if (navigable.canNavigate()) navigable.navigate(true)
          }
        }
      })
    }
  }

  override fun startInWriteAction(): Boolean = false
}
