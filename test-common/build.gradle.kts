@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.failgoodVersion

plugins {
    kotlin("jvm")
    id("restaurant.common")
}

dependencies {
    api("dev.failgood:failgood:$failgoodVersion")
    api(kotlin("test"))
    api(kotlin("test-junit5")) // this improves assertEquals output and makes idea show a diff
    implementation("ch.qos.logback:logback-classic:1.5.16")
}

