import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0-RC"
}
val failfastVersion = "0.4.0"
val striktVersion = "0.30.1"
val okhttpVersion = "4.9.1"

group = "me.christoph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.undertow:undertow-core:2.2.7.Final")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("com.christophsturm.failfast:failfast:$failfastVersion")

}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
