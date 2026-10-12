package com.intellij.ideolog.file

import com.intellij.ideolog.largeFile.LargeLogFileEditorProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.withValue
import com.intellij.openapi.vfs.limits.FileSizeLimit
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightModuleFixture
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@TestApplication
@TestDataPath($$"$PROJECT_ROOT/plugins/ideolog/src/test/resources/highlighting")
class LogFileEditorTest {
  companion object {
    private val projectFixture = projectFixture(openAfterCreation = true)
  }

  private val myFixture by codeInsightModuleFixture(projectFixture)
  private val project get() = myFixture.project

  @Test
  fun testLogFile(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val logFile = myFixture.addFileToProject("LogFile.log", "")
      assertTrue(LogFileEditorProvider().accept(project, logFile.virtualFile))
      assertEquals(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile).size)
    }
  }

  @Test
  fun testLogFileUppercaseExtension(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val logFile = myFixture.addFileToProject("LogFile.LOG", "")
      assertTrue(LogFileEditorProvider().accept(project, logFile.virtualFile))
      assertEquals(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile).size)
    }
  }

  @Test
  fun testNotLogFile(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val logFile = myFixture.addFileToProject("LogFile.notLog", "")
      assertFalse(LogFileEditorProvider().accept(project, logFile.virtualFile))
    }
  }

  @Test
  fun testLargeLogFile(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      Registry.get("ideolog.large.file.editor.enabled").withValue(true) {
        val logFile = myFixture.addFileToProject("LargeLogFile.log", "a".repeat(FileSizeLimit.getContentLoadLimit("log") + 1))
        assertTrue(LargeLogFileEditorProvider().accept(project, logFile.virtualFile))
        assertEquals(1, FileEditorProviderManagerImpl().getProviderList(project, logFile.virtualFile).size)
      }
    }
  }
}
