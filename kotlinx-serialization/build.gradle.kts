@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.failgoodVersion
import restaurant.versions.striktVersion

plugins {
    kotlin("jvm")
//    id("info.solidsoft.pitest") // disable because this does not currently work
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jetbrains.kotlinx.kover")
    kotlin("plugin.serialization") version ("2.1.10")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    api(project(":restaurant-rest"))
    kotlin("test")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation(project(":restaurant-test-common"))
}
