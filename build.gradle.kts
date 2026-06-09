import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "de.nicerdicer"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://snapshots.kord.dev")
}

dependencies {
    implementation("dev.kord:kord-core:0.18.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("de.nicerdicer.Main")
}