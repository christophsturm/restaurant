@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "restaurant-root"
include("restaurant-core", "restaurant-jackson", "restaurant-jwt")
project(":restaurant-core").projectDir = file("core")
project(":restaurant-jackson").projectDir = file("jackson")
project(":restaurant-jwt").projectDir = file("jwt")
