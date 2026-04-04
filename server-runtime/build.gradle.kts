@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.kmp") }

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":restaurant-api"))
            api(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
