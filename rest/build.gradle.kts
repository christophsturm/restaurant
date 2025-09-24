@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    id("shared.pitest")
    id("shared.common")
    id("shared.publishing")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(project(":restaurant-core"))
    implementation(libs.kotlin.reflect)
    testImplementation(libs.failgood)
}
