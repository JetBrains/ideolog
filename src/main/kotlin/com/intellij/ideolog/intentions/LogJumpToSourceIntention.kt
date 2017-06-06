package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.find.ngrams.TrigramIndex
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ManagingFS
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.cache.impl.id.IdIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.IndexInfrastructure
import com.intellij.util.indexing.UpdatableIndex
import gnu.trove.TIntObjectHashMap
import java.util.*


class FileMatch(val evt: LogEvent) {
    var levelPresent = 0
    var categoryPresent = 0
    var messageTrigramCount = 0

    var filenameMatch = 0 // 1 - prefix match to category, 2- full match to category

    lateinit private var _vf : VirtualFile
    var virtualFile : VirtualFile
        get() = _vf
        set(value) {
            _vf = value
            val name = _vf.nameWithoutExtension
            if (evt.category.isNotBlank()) {
                if (name == evt.category) filenameMatch = 2
                else if (name.startsWith(evt.category) && /*To suppress short prefixes*/name.length <= evt.category.length*1.5) {
                    filenameMatch = 1
                }
            }
        }

    val priority: Int get() = filenameMatch * 15 + categoryPresent * 3 + levelPresent*2 +  messageTrigramCount

    override fun toString(): String {
        return "Match(${virtualFile.presentableName}): (level: $levelPresent, "+
        "category: $categoryPresent, "+
        "message: $messageTrigramCount of ${evt.messageTrigrams.size}, "+
        "priority:  $priority, " +
        "filename: $filenameMatch, " +
        "best: $bestScore" +
        ")"
    }


    var bestScore = 0
    var bestLine = 0


    companion object {
        private val LIMIT = 100
        val m = Array(LIMIT) {IntArray(LIMIT)}
    }


    fun processFileOffset(psiManager: PsiManager)  {
        val psiFile = psiManager.findFile(virtualFile)?: return
        val text = psiFile.text
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
                    }
                    else {
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
        return "Jump To Source"
    }

    override fun getFamilyName() = "Logs"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (file?.fileType == LogFileType)
    }


    override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
        doIt(project, editor)
    }


    companion object {
        private fun getFilesToMatch(project: Project, evt: LogEvent) : List<FileMatch> {
            val filtered = ArrayList<FileMatch>()

            ApplicationManager.getApplication().runReadAction {
                val indexManager = FileBasedIndex.getInstance() as FileBasedIndexImpl
                val trigramIndex = indexManager.getIndex(TrigramIndex.INDEX_ID)


                val fileIdMap = TIntObjectHashMap<FileMatch>()

                trigramIndex.populateFileIdMap(fileIdMap, evt, evt.messageTrigrams) {it.messageTrigramCount ++ }


                val filesWithCategory = indexManager.getContainingFiles(IdIndex.NAME, evt.categoryIdEntry, GlobalSearchScope.projectScope(project))
                val filesWithLevel = indexManager.getContainingFiles(IdIndex.NAME, evt.levelIdEntry, GlobalSearchScope.projectScope(project))

                val fs = ManagingFS.getInstance() as PersistentFS
                val pfi = ProjectFileIndex.SERVICE.getInstance(project)


                fileIdMap.forEachEntry { fileId, match ->
                    val vf = IndexInfrastructure.findFileByIdIfCached(fs, fileId)
                    if (vf == null
                        || !pfi.isInSourceContent(vf)
                        || vf.extension !in listOf("kt", "java", "cs", "vb", "cpp")
                        ) {
                        return@forEachEntry true
                    }



                    match.virtualFile = vf

                    if (filesWithCategory.contains(vf)) {
                        match.categoryPresent = 1
                        filesWithCategory.remove(vf)
                    }

                    if (filesWithLevel.contains(vf)) {
                        match.levelPresent = 1
                        filesWithLevel.remove(vf)
                    }

                    filtered.add(match)

                    true
                }


                filesWithCategory.forEach { vf ->
                    if (vf == null
                        || vf.extension == LogFileType.defaultExtension
                        || !pfi.isInSourceContent(vf)) {
                        return@forEach
                    }

                    val match = FileMatch(evt)
                    match.virtualFile = vf
                    match.categoryPresent = 1

                    if (filesWithLevel.contains(vf)) {
                        match.levelPresent = 1
                        filesWithLevel.remove(vf)
                    }

                    filtered.add(match)
                }
            }



            filtered.sortBy { -it.priority }

            return filtered
        }

        private inline fun <K, V, I> UpdatableIndex<K, V, I>.populateFileIdMap(
            fileIdMap: TIntObjectHashMap<FileMatch>,
            evt: LogEvent, dataKeys: Collection<K>,
            inc: (FileMatch) -> Unit
        ) {
          @Suppress("LoopToCallChain")
          for (dataKey in dataKeys) {
              val valueIterator = this.getData(dataKey).valueIterator
              if(!valueIterator.hasNext())
                  continue
              val it = valueIterator.inputIdsIterator
              while (it.hasNext()) {
                  val fileId = it.next()
                  var structure = fileIdMap.get(fileId)
                  if (structure == null) {
                      structure = FileMatch(evt)
                      fileIdMap.put(fileId, structure)
                  }

                  inc(structure)
              }
          }
        }


        fun doIt(project: Project, editor: Editor) {
            println("\n<JumpToSource>")
            if (!TrigramIndex.ENABLED) {
                System.err.println("Trigram index is disabled")
                return
            }


            val event = LogEvent.fromEditor(editor)
            event.prepareTrigrams()
            println(event)


            if (event.message.isNullOrBlank()) return

            val sorted = getFilesToMatch(project, event)

            val bestCandidates = sorted.take(3).toMutableList()


            val psiManager = PsiManager.getInstance(project)

            println("Best candidates:")
            bestCandidates.forEach {
                it.processFileOffset(psiManager)
                println(it)
            }



            bestCandidates.sortBy { - it.bestScore }
            val best = bestCandidates.firstOrNull()?: return



            val descriptor = OpenFileDescriptor(project, best.virtualFile, best.bestLine, 0)
            val navigable = descriptor.setUseCurrentWindow(true)
            if (navigable.canNavigate()) navigable.navigate(true)
        }
    }

    override fun startInWriteAction() = false
}
