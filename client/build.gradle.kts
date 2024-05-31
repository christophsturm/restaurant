@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.coroutinesVersion
import restaurant.versions.kotlinVersion

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

