@file:Suppress("GradlePackageUpdate") // buggy

plugins { id("buildgood.module") }

dependencies {
    implementation(platform(libs.kotlin.bom))
    api(project(":restaurant-core"))
    api(libs.auth0.jwt)
    testImplementation(libs.strikt)
    testImplementation(project(":restaurant-java11-client"))
    testImplementation(project(":restaurant-test-common"))
    testImplementation(project(":restaurant-undertow"))
}
