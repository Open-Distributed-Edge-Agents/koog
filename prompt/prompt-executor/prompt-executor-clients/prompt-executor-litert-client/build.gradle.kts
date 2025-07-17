import ai.koog.gradle.publish.maven.Publishing.publishToMaven

plugins {
    id("ai.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

group = rootProject.group
version = rootProject.version

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":prompt:prompt-executor:prompt-executor-clients"))
                api(project(":prompt:prompt-llm"))
                api(project(":prompt:prompt-executor:prompt-executor-model"))
                api(project(":prompt:prompt-model"))

                api(libs.tensorflow.lite.task.vision)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.oshai.kotlin.logging)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    explicitApi()
}

publishToMaven()
