@file:Suppress("GradlePackageUpdate")

import restaurant.versions.failgoodVersion

// buggy

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("restaurant.publish")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(project(":restaurant-core"))
    implementation(kotlin("reflect"))
    kotlin("test")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
}
