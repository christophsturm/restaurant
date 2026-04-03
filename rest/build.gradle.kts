@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(project(":restaurant-core"))
    implementation(libs.kotlin.reflect)
    testImplementation(libs.failgood)
}
