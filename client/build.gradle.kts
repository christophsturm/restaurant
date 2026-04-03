@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.module") }

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
}
