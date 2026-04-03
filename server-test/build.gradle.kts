@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-undertow"))
    testImplementation(project(":restaurant-test-common"))
}
