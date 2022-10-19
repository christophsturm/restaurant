@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

val projectName = "restaurant"
rootProject.name = "$projectName-root"
val modules = listOf("core", "jackson", "jwt", "kotlinx-serialization")
val projects = modules.map {"$projectName-$it"}
include(modules.map { "$projectName-$it" })
modules.forEach {
    project(":$projectName-$it").projectDir = file(it)
}
//includeBuild("../failgood")
