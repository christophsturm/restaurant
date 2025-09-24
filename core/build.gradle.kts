@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    id("shared.pitest")
    id("shared.common")
    id("shared.publishing")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-client"))

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))

    api(libs.undertow.core)

    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-test-common"))
}
