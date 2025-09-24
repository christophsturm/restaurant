@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    id("shared.pitest")
    id("shared.common")
    id("shared.publishing")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(libs.jackson.core)
    api(libs.jackson.databind)

    implementation(libs.jackson.module.kotlin)
    implementation(platform(libs.kotlin.bom))
    api(project(":restaurant-rest"))
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-test-common"))
}
