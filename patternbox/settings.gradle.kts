pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "patternbox"

// :core is plain kotlin (patterns, stats, the whole fight sim) so it builds and tests without an android sdk
include(":core")
include(":app")
