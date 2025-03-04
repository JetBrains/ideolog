package com.intellij.ideolog.statistics

import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import java.util.*

private enum class AllowedLogFileFormat(val value: String) {
  LARAVEL("Laravel"),
  SYMFONY("Symfony"),
  WORDPRESS("WordPress"),
  CUSTOM("Custom"),
  UNDETECTED("Undetected");
}

private object LogFileFormatValues {
  val phpValues: HashSet<String> = listOf(AllowedLogFileFormat.LARAVEL, AllowedLogFileFormat.SYMFONY, AllowedLogFileFormat.WORDPRESS)
    .map(AllowedLogFileFormat::value)
    .toHashSet()
  val allowedValues: List<String> = AllowedLogFileFormat.entries.map(AllowedLogFileFormat::value)
}

object IdeologUsagesCollector : CounterUsagesCollector() {
  private val GROUP = EventLogGroup("ideolog", 3)

  private val LOG_FILE_FORMAT_FIELD = EventFields.String("log_file_format", LogFileFormatValues.allowedValues)

  private val LOG_FILE_OPENED_IN_TERMINAL = GROUP.registerEvent("log.file.in.terminal.opened")
  private val DETECTED_LOG_FILE_FORMAT = GROUP.registerVarargEvent("log.file.format.detected",
                                                                   LOG_FILE_FORMAT_FIELD)
  private val EXPLAIN_WITH_AI_ACTION_BUTTON_CLICKED = GROUP.registerEvent("ai.action.button.clicked")

  fun logOpenLogFileInTerminal(project: Project) {
    LOG_FILE_OPENED_IN_TERMINAL.log(project)
  }

  fun logDetectedLogFormat(logFileFormat: LogFileFormat?) {
    val logFileFormatAllowedValue = when (val logFileFormatName = logFileFormat?.myRegexLogParser?.otherParsingSettings?.name) {
      null -> AllowedLogFileFormat.UNDETECTED
      in LogFileFormatValues.phpValues -> AllowedLogFileFormat.valueOf(logFileFormatName.uppercase(Locale.getDefault()))
      else -> AllowedLogFileFormat.CUSTOM
    }
    DETECTED_LOG_FILE_FORMAT.log(
      LOG_FILE_FORMAT_FIELD.with(logFileFormatAllowedValue.value)
    )
  }

  fun logExplainWithAiActionButtonClicked(project: Project) {
    EXPLAIN_WITH_AI_ACTION_BUTTON_CLICKED.log(project)
  }

  override fun getGroup(): EventLogGroup = GROUP
}
