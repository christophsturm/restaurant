import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    id("com.github.ben-manes.versions") version "0.45.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.autonomousapps.dependency-analysis") version "1.19.0"
    id("org.jetbrains.kotlinx.kover") version "0.6.1" apply false
    id("org.jmailen.kotlinter") version "3.13.0" apply false
}
// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
    // optional parameters
    gradleReleaseChannel = "current"
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

nexusPublishing {
    repositories {
        sonatype {
            packageGroup.set("com.christophsturm.failfast")
        }
    }
}

tasks.register<LintTask>("lintBuildscripts") {
    group = "verification"
    source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}
tasks.register<FormatTask>("formatBuildscripts") {
    group = "verification"
    source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}
