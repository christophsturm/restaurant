import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
    java
    `maven-publish`
    signing
    id("com.ncorti.ktfmt.gradle")
}


tasks {
    test {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
        testLogging {
            setExceptionFormat("full")
            setEvents(listOf("passed", "skipped", "failed", "standardOut", "standardError"))
        }
    }
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11 // need at least jdk 11 for the http11 httpclient anyway
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
kotlin {
    sourceSets.all {
        languageSettings.apply {
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
@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertNotNull",
        "failgood.softly.AssertDSL.assert"
    )
}
ktfmt {
    kotlinLangStyle()
    manageTrailingCommas = false
}
