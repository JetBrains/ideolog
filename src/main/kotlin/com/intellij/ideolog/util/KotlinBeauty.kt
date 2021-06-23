package com.intellij.ideolog.util

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

inline fun <reified T> getService(): T = ServiceManager.getService(T::class.java)

val application: Application get() = ApplicationManager.getApplication()
