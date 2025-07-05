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

includeBuild("../..") { // Include the main Koog build
    dependencySubstitution {
        // This block can be used if you need to substitute dependencies from the main build,
        // e.g., if the main build publishes artifacts that this build consumes.
        // For direct project dependencies, this might not be strictly needed if paths are resolvable.
    }
}

include(":app")
