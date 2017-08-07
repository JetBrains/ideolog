package com.intellij.ideolog.fileType

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

class LogFileTypeFactory : FileTypeFactory() {
  private val LOG_FILE_EXTENSIONS = "log" // add more extensions separated with ;

  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(LogFileType, LOG_FILE_EXTENSIONS)
  }
}
