@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    //    id("info.solidsoft.pitest") // this has only test sources so nothing to mutate so no
    // pitest
    id("shared.common")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities") version "0.11.1"
}

dependencies {
    api(project(":restaurant-core"))
    testImplementation(project(":restaurant-test-common"))
}
