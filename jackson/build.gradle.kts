@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
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
