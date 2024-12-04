@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}
plugins {
    id("com.autonomousapps.build-health") version "2.4.2"
    id("org.jetbrains.kotlin.jvm") version "2.1.0" apply false
}

val projectName = "restaurant"
rootProject.name = "$projectName-root"
val modules = listOf("test-common", "client", "core", "rest", "jackson", "kotlinx-serialization", "jwt", "server-test")
val projects = modules.map { "$projectName-$it" }
include(modules.map { "$projectName-$it" })
modules.forEach {
    project(":$projectName-$it").projectDir = file(it)
}
