import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  val kotlinVersion = "1.4.21"

  repositories {
    mavenCentral()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins {
  id("me.filippov.gradle.jvm.wrapper") version "0.9.3"
  id("org.jetbrains.intellij") version "0.6.5"
}

apply(plugin = "kotlin")

repositories {
  mavenCentral()
  maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

group = "ideolog"
val buildNumber: String by rootProject.extra
version = buildNumber

intellij {
  version = "2020.3"
  pluginName = "ideolog"
  tasks {
    withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
      updateSinceUntilBuild = true
      setUntilBuild("")
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.allWarningsAsErrors = true
  kotlinOptions.freeCompilerArgs += "-Xnew-inference"
  kotlinOptions.jvmTarget = "11"
}
