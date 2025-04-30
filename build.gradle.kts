import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java")
  id("idea")
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
  id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "ideolog"
version = "2025.1"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  compilerOptions {
    allWarningsAsErrors = true
    jvmTarget = JvmTarget.JVM_21
  }
}

dependencies {
  intellijPlatform {
    create("IU", "2025.1", useInstaller = true)

    bundledPlugin("org.jetbrains.kotlin")
    bundledPlugin("com.intellij.java")

    pluginVerifier()
    testFramework(TestFrameworkType.Platform)
  }

  testImplementation("junit:junit:4.13.2")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")
}

intellijPlatform {
  pluginConfiguration {
    name = "ideolog"
    ideaVersion {
      sinceBuild = "251"
      untilBuild = "251.*"
    }
  }
  tasks {
    runIde {
      systemProperties["idea.is.internal"] = "true"
    }
  }
}

tasks {
  wrapper {
    gradleVersion = "8.1.1"
  }

  buildSearchableOptions {
    enabled = false
    runtimeDirectory
  }
}
