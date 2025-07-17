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
        substitute(module("com.jetbrains.koog:koog-agents")).using(project(":koog-agents"))
        substitute(module("com.jetbrains.koog:prompt-executor-llms-all")).using(project(":prompt:prompt-executor:prompt-executor-llms-all"))
    }
}
