plugins {
    id("ai.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

group = rootProject.group
version = rootProject.version

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    sourceSets {
        commonMain.dependencies {
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
            implementation(libs.oshai.kotlin.logging)
        }
        androidMain.dependencies {
            api(libs.google.mediapipe.tasks.genai)
        }
    }
}
