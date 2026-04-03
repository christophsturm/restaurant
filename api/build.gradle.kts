@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.kmp") }

kotlin {
    jvm()
    sourceSets { commonMain.dependencies { api(libs.kotlinx.coroutines.core) } }
}
