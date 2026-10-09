import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "5.2.1"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "nada.MainVerticle"
val launcherClassName = "io.vertx.launcher.application.VertxApplication"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-launcher-application")
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-web")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.21.7")
  implementation("tools.jackson.core:jackson-databind:3.1.7")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf(mainVerticleName)
}
