@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.coroutinesVersion
import restaurant.versions.failgoodVersion
import restaurant.versions.kotlinVersion
import restaurant.versions.log4j2Version
import restaurant.versions.striktVersion
import restaurant.versions.undertowVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))

    implementation(kotlin("reflect"))
    api("io.undertow:undertow-core:$undertowVersion")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.github.microutils:kotlin-logging:2.1.23")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testRuntimeOnly("org.slf4j:slf4j-api:1.7.36")
}


