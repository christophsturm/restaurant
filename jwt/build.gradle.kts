@file:Suppress("GradlePackageUpdate") // buggy
import restaurant.versions.failgoodVersion
import restaurant.versions.kotlinVersion
import restaurant.versions.log4j2Version
import restaurant.versions.striktVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jmailen.kotlinter")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    api(project(":restaurant-core"))
    api("com.auth0:java-jwt:4.2.2")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.6")
}
