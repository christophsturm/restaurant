@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.module")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-api"))
    api(project(":restaurant-client"))

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))

    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-java11-client"))
    testImplementation(project(":restaurant-netty"))
    testImplementation(project(":restaurant-okhttp-client"))
    testImplementation(project(":restaurant-test-common"))
    testImplementation(project(":restaurant-undertow"))
}
