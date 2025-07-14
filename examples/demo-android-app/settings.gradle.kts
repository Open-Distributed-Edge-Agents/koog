pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "koog-demo-android-app"

include(":app")

// Include koog-agents module and its submodules
include(":koog-agents")

include(
    ":agents:agents-core",
    ":agents:agents-ext",
    ":agents:agents-features:agents-features-common",
    ":agents:agents-features:agents-features-event-handler",
    ":agents:agents-features:agents-features-memory",
    ":agents:agents-features:agents-features-opentelemetry",
    ":agents:agents-features:agents-features-snapshot",
    ":agents:agents-features:agents-features-tokenizer",
    ":agents:agents-features:agents-features-trace",
    ":agents:agents-mcp",
    ":agents:agents-test",
    ":agents:agents-tools",
    ":agents:agents-utils",
)

// Include prompt module and its submodules
include(
    ":prompt:prompt-cache:prompt-cache-files",
    ":prompt:prompt-cache:prompt-cache-model",
    ":prompt:prompt-cache:prompt-cache-redis",
    ":prompt:prompt-executor:prompt-executor-cached",
    ":prompt:prompt-executor:prompt-executor-clients",
    ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-litert-client",
    ":prompt:prompt-executor:prompt-executor-llms",
    ":prompt:prompt-executor:prompt-executor-llms-all",
    ":prompt:prompt-executor:prompt-executor-model",
)
