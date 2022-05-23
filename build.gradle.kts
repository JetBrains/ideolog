import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  val kotlinVersion = "1.4.32"

  repositories {
    mavenCentral()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins {
  id("me.filippov.gradle.jvm.wrapper") version "0.9.3"
  id("org.jetbrains.intellij") version "1.6.0"
}

dependencies {
  implementation(kotlin("test"))
  implementation("org.junit.jupiter:junit-jupiter:5.8.2")
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
  version.set("2020.3")
  pluginName.set("ideolog")
  tasks {
    withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
      updateSinceUntilBuild.set(true)
      untilBuild.set("")
    }
    runIde {
      systemProperty("idea.is.internal", "true")
    }
    test {
      useJUnitPlatform()
    }
  }

}

tasks.withType<KotlinCompile> {
  kotlinOptions.allWarningsAsErrors = true
  kotlinOptions.freeCompilerArgs += "-Xnew-inference"
  kotlinOptions.jvmTarget = "11"
}
