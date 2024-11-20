@file:Suppress("GradlePackageUpdate") // buggy

import restaurant.versions.failgoodVersion

plugins {
    kotlin("jvm")
    id("restaurant.common")
}

dependencies {
    api("dev.failgood:failgood:$failgoodVersion")
    api(kotlin("test"))
    implementation("ch.qos.logback:logback-classic:1.5.12")
}

