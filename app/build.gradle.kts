import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
  // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
  kotlin("jvm") version "1.4.32"
  kotlin("plugin.serialization") version "1.4.32"

  // Apply the application plugin to add support for building a CLI application in Java.
  application
}

group = "net.maxxsoft"
version = "0.0.3"

repositories {
  maven(url = "https://mirrors.huaweicloud.com/repository/maven")

  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  // Use the Kotlin JDK 8 standard library.
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  // This dependency is used by the application.
  implementation("com.google.guava:guava:30.1-jre")

  // Use the Kotlin test library.
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  // Use the Kotlin JUnit integration.
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

  // SLF4J.
  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("org.slf4j:slf4j-simple:1.7.30")

  // Word 中文分词.
  implementation("org.ansj:ansj_seg:5.1.1")

  // SQLite JDBC.
  implementation("org.xerial:sqlite-jdbc:3.34.0")

  // Mirai
  api("net.mamoe", "mirai-core-api", "2.6.7")
  runtimeOnly("net.mamoe", "mirai-core", "2.6.7")
}

application {
  // Define the main class for the application.
  mainClass.set("net.maxxsoft.chii.AppKt")
  applicationDefaultJvmArgs = listOf("-Dmirai.slider.captcha.supported")
}

tasks.withType(KotlinJvmCompile::class.java) {
  kotlinOptions.jvmTarget = "1.8"
}
