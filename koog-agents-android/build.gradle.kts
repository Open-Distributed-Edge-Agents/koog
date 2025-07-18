plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

group = rootProject.group
version = rootProject.version

android {
    namespace = "ai.koog.agents.android"
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
                implementation(project(":koog-agents"))
                implementation(project(":prompt:prompt-executor:prompt-executor-litert"))
            }
        }
    }
}
