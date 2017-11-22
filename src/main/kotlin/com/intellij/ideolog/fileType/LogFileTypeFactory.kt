package com.intellij.ideolog.fileType

import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

class LogFileTypeFactory : FileTypeFactory() {
  private val LOG_FILE_EXTENSIONS = "log" // add more extensions separated with ;

  override fun createFileTypes(consumer: FileTypeConsumer) {
    LOG_FILE_EXTENSIONS.split(";").forEach {
      consumer.consume(LogFileType, object : FileNameMatcher {
        private val pattern = Regex("^.*\\.$it(.\\d+)?$")

        override fun accept(fileName: String): Boolean = pattern.matches(fileName)

        override fun getPresentableString(): String = it
      })
    }
  }
}
