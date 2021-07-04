import restaurant.versions.coroutinesVersion
import restaurant.versions.failgoodVersion
import restaurant.versions.kotlinVersion
import restaurant.versions.log4j2Version
import restaurant.versions.striktVersion
import restaurant.versions.undertowVersion

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")

}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))

    implementation(kotlin("reflect"))
    // temp api deps
    implementation("io.undertow:undertow-core:$undertowVersion")
    implementation("io.github.microutils:kotlin-logging:2.0.8")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
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

