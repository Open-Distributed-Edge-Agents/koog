plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ai.koog.prompt.executor.litert"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":prompt:prompt-executor:prompt-executor-model"))
    implementation(project(":prompt:prompt-llm"))
    implementation(libs.google.mediapipe.tasks.genai)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.oshai.kotlin.logging)

    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockk)
}
