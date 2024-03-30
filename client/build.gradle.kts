@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.coroutinesVersion
import restaurant.versions.failgoodVersion
import restaurant.versions.kotlinVersion
import restaurant.versions.log4j2Version
import restaurant.versions.striktVersion
import restaurant.versions.undertowVersion

plugins {
    kotlin("jvm")
    id("restaurant.common")
    id("restaurant.publish")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

}

