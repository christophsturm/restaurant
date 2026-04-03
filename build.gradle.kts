import buildgood.CommonBuildExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8" apply false
    id("buildgood.root")
}
// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository)

// Configure build settings for all modules using the DSL
commonBuild {
    basePackage = "restaurant"
    jvmTarget {
        production(JvmTarget.JVM_11)
        test(JvmTarget.JVM_17)
    }
    // Don't use strict mode based on gradle.properties setting
}

nexusPublishing {
    repositories {
        sonatype {
            packageGroup.set("com.christophsturm.failfast")
        }
    }
}
/* todo: create ktfmt version
tasks.register<LintTask>("lintBuildscripts") {
    group = "verification"
    source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}
tasks.register<FormatTask>("formatBuildscripts") {
    group = "verification"
    source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}
*/
