@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.kmp") }

kotlin {
    jvm()
    iosSimulatorArm64()
    sourceSets { commonMain.dependencies { api(libs.kotlinx.coroutines.core) } }
}
