package com.intellij.ideolog.fileType

class LogFileTypeFactory : com.intellij.openapi.fileTypes.FileTypeFactory() {
  private val extList = listOf("log")
  private val matchers = extList.map { com.intellij.openapi.fileTypes.ExtensionFileNameMatcher(it) }

  override fun createFileTypes(consumer: com.intellij.openapi.fileTypes.FileTypeConsumer) {
    for (matcher in matchers) {
      consumer.consume(LogFileType, matcher)
    }
    consumer.consume(LogFileType)
  }
}
