import ai.koog.gradle.publish.maven.Publishing.publishToMaven

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

group = rootProject.group
version = rootProject.version

android {
    namespace = "ai.koog.prompt.executor.litert"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":agents:agents-tools"))
                api(project(":prompt:prompt-llm"))
                api(project(":prompt:prompt-model"))
                api(project(":agents:agents-tools"))
                api(project(":prompt:prompt-executor:prompt-executor-model"))
                api(project(":prompt:prompt-executor:prompt-executor-clients"))
                api(project(":prompt:prompt-executor:prompt-executor-llms"))
                api(project(":embeddings:embeddings-base"))

                api(libs.ktor.client.logging)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.client.cio)
                implementation(libs.oshai.kotlin.logging)
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.mediapipe.tasks.genai)
                api(libs.tflite.java)
            }
        }
    }
}

publishToMaven()
