@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.*

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jetbrains.kotlinx.kover")
    id("org.jmailen.kotlinter")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    api(project(":restaurant-rest"))
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.14")
}
