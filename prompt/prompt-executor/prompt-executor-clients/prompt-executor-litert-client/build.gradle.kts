import ai.koog.gradle.publish.maven.Publishing.publishToMaven

plugins {
    alias(libs.plugins.android.library) // Use alias from libs.versions.toml
    id("ai.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
    // org.jetbrains.kotlin.android should be applied by ai.kotlin.multiplatform for Android targets
}

group = rootProject.group
version = rootProject.version

android { // Added Android configuration
    namespace = "ai.koog.prompt.executor.litert.client"
    compileSdk = 34 // Or your project's compileSdk

    defaultConfig {
        minSdk = 24 // Or your project's minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Packaging options to avoid conflicts with native libraries if they arise
    packagingOptions {
        pickFirsts.add("lib/x86_64/libc++_shared.so")
        pickFirsts.add("lib/x86/libc++_shared.so")
        pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
        pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    androidTarget { // Added Android target
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":agents:agents-tools"))
                api(project(":prompt:prompt-llm"))
                api(project(":prompt:prompt-model"))
                api(project(":prompt:prompt-executor:prompt-executor-model"))
                api(project(":prompt:prompt-executor:prompt-executor-clients"))

                api(libs.kotlinx.coroutines.core)
                implementation(libs.oshai.kotlin.logging)
            }
        }

        val androidMain by getting { // Changed from commonMain to androidMain for Android-specific code
            dependencies {
                api("com.google.mediapipe:tasks-genai:0.10.24") // LiteRT dependency
                api(libs.ktor.client.android) // Ktor client for Android if needed, or remove
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting // Already defined, just addingandroidTestImplementation here for clarity

        val androidTest by getting { // DefineandroidTest source set for instrumentation tests
            dependencies {
                implementation(kotlin("test-junit")) // For JUnit assertions
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.kotlinx.coroutines.test) // For testing coroutines
            }
        }
    }

    explicitApi()
}

publishToMaven()
