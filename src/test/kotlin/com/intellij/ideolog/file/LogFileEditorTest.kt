package com.intellij.ideolog.file

import com.intellij.ideolog.largeFile.LargeLogFileEditorProvider
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.withValue
import com.intellij.openapi.vfs.limits.FileSizeLimit
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import java.nio.file.Path
import kotlin.io.path.pathString

class LogFileEditorTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    Path.of(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins/ideolog/src/test/resources/highlighting").pathString

  fun testLogFile() {
    val logFile = myFixture.addFileToProject("LogFile.log", "")
    assertTrue(LogFileEditorProvider().accept(project, logFile.virtualFile))
    assertSize(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile))
  }

  fun testLogFileUppercaseExtension() {
    val logFile = myFixture.addFileToProject("LogFile.LOG", "")
    assertTrue(LogFileEditorProvider().accept(project, logFile.virtualFile))
    assertSize(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile))
  }

  fun testNotLogFile() {
    val logFile = myFixture.addFileToProject("LogFile.notLog", "")
    assertFalse(LogFileEditorProvider().accept(project, logFile.virtualFile))
  }

  fun testLargeLogFile() {
    Registry.get("ideolog.large.file.editor.enabled").withValue(true) {
      val logFile = myFixture.addFileToProject("LargeLogFile.log", "a".repeat(FileSizeLimit.getContentLoadLimit("log") + 1))
      assertTrue(LargeLogFileEditorProvider().accept(project, logFile.virtualFile))
      assertSize(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile))
    }
  }
}
