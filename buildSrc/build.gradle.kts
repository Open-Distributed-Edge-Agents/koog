repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/jcs/maven")
    google()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.jetsign.gradle.plugin)
    implementation("com.android.tools.build:gradle:8.8.2")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("credentialsResolver") {
            id = "ai.koog.gradle.plugins.credentialsresolver"
            implementationClass = "ai.koog.gradle.plugins.CredentialsResolverPlugin"
        }
    }
}