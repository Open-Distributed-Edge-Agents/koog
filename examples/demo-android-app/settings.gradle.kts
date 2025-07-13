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

includeBuild("../../") {
    dependencySubstitution {
        substitute(module("ai.koog:koog-agents")).using(project(":koog-agents"))
        substitute(module("ai.koog:prompt-executor-openai-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-openai-client"))
        substitute(module("ai.koog:prompt-executor-litert-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-litert-client"))
        substitute(module("ai.koog:agents-core")).using(project(":agents:agents-core"))
        substitute(module("ai.koog:agents-ext")).using(project(":agents:agents-ext"))
        substitute(module("ai.koog:agents-features-common")).using(project(":agents:agents-features:agents-features-common"))
        substitute(module("ai.koog:agents-features-event-handler")).using(project(":agents:agents-features:agents-features-event-handler"))
        substitute(module("ai.koog:agents-features-memory")).using(project(":agents:agents-features:agents-features-memory"))
        substitute(module("ai.koog:agents-features-opentelemetry")).using(project(":agents:agents-features:agents-features-opentelemetry"))
        substitute(module("ai.koog:agents-features-tokenizer")).using(project(":agents:agents-features:agents-features-tokenizer"))
        substitute(module("ai.koog:agents-features-trace")).using(project(":agents:agents-features:agents-features-trace"))
        substitute(module("ai.koog:agents-mcp")).using(project(":agents:agents-mcp"))
        substitute(module("ai.koog:agents-test")).using(project(":agents:agents-test"))
        substitute(module("ai.koog:agents-tools")).using(project(":agents:agents-tools"))
        substitute(module("ai.koog:agents-utils")).using(project(":agents:agents-utils"))
        substitute(module("ai.koog:embeddings-base")).using(project(":embeddings:embeddings-base"))
        substitute(module("ai.koog:embeddings-llm")).using(project(":embeddings:embeddings-llm"))
        substitute(module("ai.koog:prompt-cache-files")).using(project(":prompt:prompt-cache:prompt-cache-files"))
        substitute(module("ai.koog:prompt-cache-model")).using(project(":prompt:prompt-cache:prompt-cache-model"))
        substitute(module("ai.koog:prompt-cache-redis")).using(project(":prompt:prompt-cache:prompt-cache-redis"))
        substitute(module("ai.koog:prompt-executor-cached")).using(project(":prompt:prompt-executor:prompt-executor-cached"))
        substitute(module("ai.koog:prompt-executor-clients")).using(project(":prompt:prompt-executor:prompt-executor-clients"))
        substitute(module("ai.koog:prompt-executor-anthropic-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-anthropic-client"))
        substitute(module("ai.koog:prompt-executor-bedrock-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-bedrock-client"))
        substitute(module("ai.koog:prompt-executor-google-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-google-client"))
        substitute(module("ai.koog:prompt-executor-ollama-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-ollama-client"))
        substitute(module("ai.koog:prompt-executor-openrouter-client")).using(project(":prompt:prompt-executor:prompt-executor-clients:prompt-executor-openrouter-client"))
        substitute(module("ai.koog:prompt-executor-llms")).using(project(":prompt:prompt-executor:prompt-executor-llms"))
        substitute(module("ai.koog:prompt-executor-llms-all")).using(project(":prompt:prompt-executor:prompt-executor-llms-all"))
        substitute(module("ai.koog:prompt-executor-model")).using(project(":prompt:prompt-executor:prompt-executor-model"))
        substitute(module("ai.koog:prompt-llm")).using(project(":prompt:prompt-llm"))
        substitute(module("ai.koog:prompt-markdown")).using(project(":prompt:prompt-markdown"))
        substitute(module("ai.koog:prompt-model")).using(project(":prompt:prompt-model"))
        substitute(module("ai.koog:prompt-structure")).using(project(":prompt:prompt-structure"))
        substitute(module("ai.koog:prompt-tokenizer")).using(project(":prompt:prompt-tokenizer"))
        substitute(module("ai.koog:prompt-xml")).using(project(":prompt:prompt-xml"))
        substitute(module("ai.koog:rag-base")).using(project(":rag:rag-base"))
        substitute(module("ai.koog:vector-storage")).using(project(":rag:vector-storage"))
    }
}
