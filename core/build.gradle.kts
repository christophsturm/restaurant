import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("restaurant.common")

}
val failgoodVersion = "0.4.4"
val striktVersion = "0.31.0"
val okhttpVersion = "4.9.1"
val kotlinVersion = "1.5.10"
val jacksonVersion = "2.12.3"
val coroutinesVersion = "1.5.0"
val log4j2Version = "2.14.1"
val pitestVersion = "1.6.5"
val undertowVersion = "2.2.8.Final"


repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))

    implementation(kotlin("reflect"))
    implementation("io.undertow:undertow-core:$undertowVersion")
    implementation("io.github.microutils:kotlin-logging:2.0.8")
    implementation("com.auth0:java-jwt:3.16.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

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


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        languageVersion = "1.5"
        apiVersion = "1.5"
    }
}

val testMain = tasks.register("testMain", JavaExec::class) {
    mainClass.set("restaurant.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.check {
    dependsOn(testMain)
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

