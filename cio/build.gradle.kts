@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.kmp") }

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":restaurant-server-runtime"))
            implementation(libs.ktor.http)
            implementation(libs.ktor.http.cio)
            implementation(libs.ktor.network)
        }

        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
