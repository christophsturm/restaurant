@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

val projectName = "restaurant"
rootProject.name = "$projectName-root"
val modules = listOf("client", "core", "rest", "jackson", "kotlinx-serialization", "jwt", "server-test")
val projects = modules.map { "$projectName-$it" }
include(modules.map { "$projectName-$it" })
modules.forEach {
    project(":$projectName-$it").projectDir = file(it)
}
