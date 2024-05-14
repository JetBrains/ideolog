import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  val kotlinVersion = "1.9.22"

  repositories {
    mavenCentral()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins {
  id("me.filippov.gradle.jvm.wrapper") version "0.14.0"
  id("org.jetbrains.intellij") version "1.17.2"
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
  version.set("2023.3")
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
    kotlinOptions.jvmTarget = "17"
  }

  wrapper {
    gradleVersion = "8.1.1"
  }
}
