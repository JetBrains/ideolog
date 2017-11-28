package com.intellij.ideolog.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
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
    return "Logs"
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    editor ?: return false

    if (file?.fileType != LogFileType)
      return false

    return detectLogFileFormat(editor).myRegexLogParser == null
  }

  override fun getText(): String {
    return "Show log highlighting settings"
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    ShowSettingsUtil.getInstance().editConfigurable(project, LogHighlightingConfigurable())
  }
}
