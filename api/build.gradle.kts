@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.kmp") }

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    sourceSets { commonMain.dependencies { api(libs.kotlinx.coroutines.core) } }
}
