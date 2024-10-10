@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.failgoodVersion
import restaurant.versions.striktVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jetbrains.kotlinx.kover")
    id("org.jmailen.kotlinter")
    kotlin("plugin.serialization") version ("2.0.21")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    api(project(":restaurant-rest"))
    kotlin("test")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation(project(":restaurant-test-common"))
}
