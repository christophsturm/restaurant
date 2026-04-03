@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(project(":restaurant-core"))

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.undertow.core)

    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-test-common"))
}
