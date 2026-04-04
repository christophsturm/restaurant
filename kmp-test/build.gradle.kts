@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.kmp")
    kotlin("plugin.serialization") version ("2.3.20")
}

kotlin {
    jvm()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":restaurant-api"))
            implementation(project(":restaurant-kotlinx-serialization"))
        }

        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
