import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    java
    `maven-publish`
    signing
}


tasks {
    test {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
    }
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11" // need at least jdk 11 for the http11 httpclient anyway
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            languageVersion = "1.7"
            apiVersion = "1.7"
        }
    }
}
kotlin {
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.7"
            progressiveMode = true
        }
    }
}
java {
    withJavadocJar()
    withSourcesJar()
}



plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        targetClasses.set(setOf("restaurant.*")) //by default "${project.group}.*"
        targetTests.set(setOf("restaurant.*Test", "restaurant.**.*Test"))
        pitestVersion.set(restaurant.versions.pitestVersion)
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
