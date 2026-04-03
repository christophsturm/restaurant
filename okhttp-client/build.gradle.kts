@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.module") }

dependencies {
    api(project(":restaurant-client"))

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.okhttp)
}
