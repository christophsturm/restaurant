@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    id("shared.common")
    id("shared.publishing")
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
}
