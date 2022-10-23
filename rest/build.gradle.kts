@file:Suppress("GradlePackageUpdate")

// buggy

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")
    id("org.jetbrains.kotlinx.kover")
    id("org.jmailen.kotlinter")
}

dependencies {
    api(project(":restaurant-core"))
    implementation(kotlin("reflect"))
}
