package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ideolog.IdeologBundle
import com.intellij.ideolog.fileType.LogFileType
import com.intellij.ideolog.highlighting.settings.LogHighlightingConfigurable
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowIdeologSettingsIntention: IntentionAction, HighPriorityAction {
  override fun startInWriteAction(): Boolean {
    return false
  }

  override fun getFamilyName(): String {
    return IdeologBundle.message("intention.family.name.logs")
  }

  override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
    editor ?: return false

    if (psiFile?.fileType != LogFileType)
      return false

    return detectLogFileFormat(editor).myRegexLogParser == null
  }

  override fun getText(): String {
    return IdeologBundle.message("intention.name.show.log.highlighting.settings")
  }

  override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
    ShowSettingsUtil.getInstance().editConfigurable(project, LogHighlightingConfigurable())
  }
}
