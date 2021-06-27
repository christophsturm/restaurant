import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import restaurant.versions.*

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")

}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))

    implementation(kotlin("reflect"))
    // temp api deps
    api("io.undertow:undertow-core:$undertowVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.github.microutils:kotlin-logging:2.0.8")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:1.7.2")
    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testImplementation("org.slf4j:slf4j-api:1.7.31")
}



val testMain = tasks.register("testMain", JavaExec::class) {
    mainClass.set("restaurant.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.check {
    dependsOn(testMain)
}

