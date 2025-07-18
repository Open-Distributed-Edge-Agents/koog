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

includeBuild("../../") {
    dependencySubstitution {
        listOf(
            ":agents:agents-core",
            ":agents:agents-ext",
            ":agents:agents-features:agents-features-common",
            ":agents:agents-features:agents-features-event-handler",
            ":agents:agents-features:agents-features-memory",
            ":agents:agents-features:agents-features-opentelemetry",
            ":agents:agents-features:agents-features-trace",
            ":agents:agents-features:agents-features-tokenizer",
            ":agents:agents-features:agents-features-snapshot",
            ":agents:agents-mcp",
            ":agents:agents-test",
            ":agents:agents-tools",
            ":agents:agents-utils",
            ":koog-agents",
            ":koog-agents-android",
            ":prompt:prompt-cache:prompt-cache-files",
            ":prompt:prompt-cache:prompt-cache-model",
            ":prompt:prompt-cache:prompt-cache-redis",
            ":prompt:prompt-executor:prompt-executor-cached",
            ":prompt:prompt-executor:prompt-executor-clients",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-anthropic-client",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-bedrock-client",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-google-client",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-openai-client",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-openrouter-client",
            ":prompt:prompt-executor:prompt-executor-clients:prompt-executor-ollama-client",
            ":prompt:prompt-executor:prompt-executor-llms",
            ":prompt:prompt-executor:prompt-executor-litert",
            ":prompt:prompt-executor:prompt-executor-llms-all",
            ":prompt:prompt-executor:prompt-executor-model",
            ":prompt:prompt-llm",
            ":prompt:prompt-markdown",
            ":prompt:prompt-model",
            ":prompt:prompt-structure",
            ":prompt:prompt-tokenizer",
            ":prompt:prompt-xml",
            ":embeddings:embeddings-base",
            ":embeddings:embeddings-llm",
            ":rag:rag-base",
            ":rag:vector-storage",
            ":koog-spring-boot-starter"
        ).forEach { projectPath ->
            val moduleName = projectPath.substring(projectPath.lastIndexOf(":") + 1)
            substitute(module("ai.koog:$moduleName")).using(project(projectPath))
        }
    }
}

include(":app")

