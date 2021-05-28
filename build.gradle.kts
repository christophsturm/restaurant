import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("info.solidsoft.pitest") version "1.6.0"

}
val failgoodVersion = "0.4.3"
val striktVersion = "0.31.0"
val okhttpVersion = "4.9.1"
val kotlinVersion = "1.5.10"
val jacksonVersion = "2.12.3"
val coroutinesVersion = "1.5.0"
val log4j2Version = "2.14.1"
val pitestVersion = "1.6.5"



group = "com.christophsturm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//    maven("https://s01.oss.sonatype.org/content/repositories/devfailgood-1000/")
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(kotlin("reflect"))
    implementation("io.undertow:undertow-core:2.2.7.Final")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.github.microutils:kotlin-logging:2.0.6")
    implementation("com.auth0:java-jwt:3.16.0")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:1.7.2")
    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testImplementation("org.slf4j:slf4j-api:1.7.30")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xuse-ir")
        languageVersion = "1.4"
        apiVersion = "1.4"
    }
}

val testMain = tasks.register("testMain", JavaExec::class) {
    main = "restaurant.AllTestsKt"
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.check {
    dependsOn(testMain)
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    val filtered =
        listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev", "eap")
            .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*.*") }
    resolutionStrategy {
        componentSelection {
            all {
                if (filtered.any { it.matches(candidate.version) }) {
                    reject("Release candidate")
                }
            }
        }
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}

plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failgood")
        targetClasses.set(setOf("restaurant.*")) //by default "${project.group}.*"
        targetTests.set(setOf("restaurant.*Test", "restaurant.**.*Test"))
        pitestVersion.set(this@Build_gradle.pitestVersion)
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
