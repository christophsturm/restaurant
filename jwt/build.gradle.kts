@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    kotlin("jvm")
    id("shared.pitest")
    id("shared.common")
    id("shared.publishing")
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    api(project(":restaurant-core"))
    api(libs.auth0.jwt)
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-test-common"))
}
