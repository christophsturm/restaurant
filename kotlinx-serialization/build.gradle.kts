@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    //    id("info.solidsoft.pitest") // disable because this does not currently work
    id("shared.common")
    id("shared.publishing")
    id("org.jetbrains.kotlinx.kover")
    kotlin("plugin.serialization") version ("2.1.10")
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
