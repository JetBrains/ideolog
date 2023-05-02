package com.intellij.ideolog.util

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager

inline fun <reified T> getService(): T = application.getService(T::class.java)

val application: Application get() = ApplicationManager.getApplication()
