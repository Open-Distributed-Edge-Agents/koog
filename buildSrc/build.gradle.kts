repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/jcs/maven")
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.jetsign.gradle.plugin)
}

// Removed explicit Java and Kotlin toolchain settings for buildSrc.
// Let it use Gradle's default or a system-provided JDK.

gradlePlugin {
    plugins {
        create("credentialsResolver") {
            id = "ai.koog.gradle.plugins.credentialsresolver"
            implementationClass = "ai.koog.gradle.plugins.CredentialsResolverPlugin"
        }
    }
}
