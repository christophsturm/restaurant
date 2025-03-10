@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.*

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-client"))

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))

    api("io.undertow:undertow-core:$undertowVersion")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation(project(":restaurant-test-common"))
}


