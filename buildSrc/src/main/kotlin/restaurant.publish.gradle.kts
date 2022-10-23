import gradle.kotlin.dsl.accessors._0fb4ceb799762099ea9d453d482ee9d2.publishing
import gradle.kotlin.dsl.accessors._0fb4ceb799762099ea9d453d482ee9d2.signing
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.java
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing

plugins {
    java
    `maven-publish`
    signing
}

val pub = "mavenJava"

publishing {
    publications {
        create<MavenPublication>(pub) {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            pom {
                description.set("rest without boilerplate")
                name.set("restaurant")
                url.set("https://github.com/christophsturm/restaurant")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("christophsturm")
                        name.set("Christoph Sturm")
                        email.set("me@christophsturm.com")
                    }
                }
                scm {
                    url.set("https://github.com/christophsturm/restaurant.git")
                }
            }
        }
    }
}
signing {
    sign(publishing.publications[pub])
}
