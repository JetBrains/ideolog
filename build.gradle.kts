import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  val kotlinVersion = "1.8.20"

  repositories {
    mavenCentral()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins {
  id("me.filippov.gradle.jvm.wrapper") version "0.14.0"
  id("org.jetbrains.intellij") version "1.13.3"
}

dependencies {
  implementation(kotlin("test"))
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
  version.set("2022.2")
  pluginName.set("ideolog")
  tasks {
    withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
      updateSinceUntilBuild.set(true)
      sinceBuild.set("222.3345")
      untilBuild.set("")
    }
    runIde {
      systemProperty("idea.is.internal", "true")
    }
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.freeCompilerArgs += "-Xnew-inference"
    kotlinOptions.jvmTarget = "17"
  }

  wrapper {
    gradleVersion = "8.1.1"
  }
}
