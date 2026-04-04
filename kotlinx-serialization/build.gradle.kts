@file:Suppress("GradlePackageUpdate") // buggy

plugins {
    id("buildgood.kmp")
    id("org.jetbrains.kotlinx.kover")
    kotlin("plugin.serialization") version ("2.3.20")
}

kotlin {
    jvm()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":restaurant-api"))
                api(libs.kotlinx.serialization.json)
            }
        }

        jvmMain.dependencies { api(project(":restaurant-core")) }

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.failgood)
                implementation(libs.strikt)
                implementation(project(":restaurant-java11-client"))
                implementation(project(":restaurant-test-common"))
                implementation(project(":restaurant-undertow"))
            }
        }
    }
}
