import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0-RC"
}
val failfastVersion = "0.4.0"
val striktVersion = "0.30.1"
val okhttpVersion = "4.9.1"
val kotlinVersion = "1.5.0-RC"

group = "com.christophsturm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation("io.undertow:undertow-core:2.2.7.Final")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("com.christophsturm.failfast:failfast:$failfastVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.1")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
