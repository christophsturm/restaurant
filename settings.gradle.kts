@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/devfailgood-1046")
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

val projectName = "restaurant"
rootProject.name = "$projectName-root"
val modules = listOf("core", "rest", "jackson", "kotlinx-serialization", "jwt")
val projects = modules.map { "$projectName-$it" }
include(modules.map { "$projectName-$it" })
modules.forEach {
    project(":$projectName-$it").projectDir = file(it)
}
// includeBuild("../failgood")
