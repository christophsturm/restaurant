@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.module") }

dependencies {
    api(libs.failgood)
    api(libs.kotlin.test)
    api(libs.kotlin.test.junit5) // this improves assertEquals output and makes idea show a diff
    implementation(libs.logback.classic)
}
