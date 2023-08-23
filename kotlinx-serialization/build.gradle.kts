@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.failgoodVersion
import restaurant.versions.log4j2Version
import restaurant.versions.striktVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("org.jetbrains.kotlinx.kover")
    id("org.jmailen.kotlinter")
    kotlin("plugin.serialization") version ("1.9.10")
    id("com.bnorm.power.kotlin-power-assert")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    api(project(":restaurant-rest"))
    kotlin("test")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.7")
    testImplementation("io.strikt:strikt-core:$striktVersion")
}
