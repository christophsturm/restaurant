import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.github.ben-manes.versions") version "0.38.0"
}
val failfastVersion = "0.4.0"
val striktVersion = "0.30.1"
val okhttpVersion = "4.9.1"
val kotlinVersion = "1.4.32"
val jacksonVersion = "2.12.3"
val coroutinesVersion = "1.4.3"



group = "com.christophsturm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(kotlin("reflect"))
    implementation("io.undertow:undertow-core:2.2.7.Final")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("com.christophsturm.failfast:failfast:$failfastVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.1")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

val testMain = tasks.register("testMain", JavaExec::class) {
    main = "restaurant.AllTestsKt"
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.check {
    dependsOn(testMain)
}
