import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  val kotlinVersion = "1.3.70"

  repositories {
    mavenCentral()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins {
  id("me.filippov.gradle.jvm.wrapper") version "0.9.3"
  id("org.jetbrains.intellij") version "0.4.16"
}

apply(plugin = "kotlin")

repositories {
  mavenCentral()
}

group = "ideolog"
val buildNumber: String by rootProject.extra
version = buildNumber

intellij {
  version = "2019.3"
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
}
