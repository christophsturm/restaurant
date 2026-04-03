@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
plugins {
    id("com.android.library") version "9.1.0" apply false
    id("com.autonomousapps.build-health") version "2.8.2"
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}

val projectName = "restaurant"
rootProject.name = "$projectName-root"
val modules =
    listOf(
        "api",
        "android-test",
        "test-common",
        "client",
        "java11-client",
        "okhttp-client",
        "core",
        "undertow",
        "netty",
        "rest",
        "jackson",
        "kotlinx-serialization",
        "jwt",
        "server-test",
    )
val projects = modules.map { "$projectName-$it" }
include(modules.map { "$projectName-$it" })
modules.forEach {
    project(":$projectName-$it").projectDir = file(it)
}

includeBuild("../failgood/build-logic")
includeBuild("../failgood")
