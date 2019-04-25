buildscript {
  val kotlinVersion = "1.3.30"

  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  }
}

plugins { id("org.jetbrains.intellij") version "0.4.8" }
apply(plugin = "kotlin")

group = "ideolog"
val buildNumber: String by rootProject.extra
version = buildNumber

intellij {
  version = "192-SNAPSHOT"
  pluginName = "ideolog"
  intellijRepo = "http://jetbrains-com-mirror.labs.intellij.net/intellij-repository"
}

repositories {
  mavenCentral()
}

dependencies {
  compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
