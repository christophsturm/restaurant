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
    id("org.jmailen.kotlinter")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-core"))
    testImplementation("dev.failgood:failgood:$failgoodVersion")
}


