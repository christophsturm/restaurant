@file:Suppress("GradlePackageUpdate") // buggy
import restaurant.versions.kotlinVersion
import restaurant.versions.striktVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    api(project(":restaurant-core"))
    api("com.auth0:java-jwt:4.5.0")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation(project(":restaurant-test-common"))
}
