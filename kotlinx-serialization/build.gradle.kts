@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
    id("org.jetbrains.kotlinx.kover")
    kotlin("plugin.serialization") version ("2.3.20")
}

dependencies {
    api(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.serialization.json)
    api(project(":restaurant-rest"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.failgood)
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-test-common"))
}
