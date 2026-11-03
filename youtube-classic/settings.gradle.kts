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
        // NewPipeExtractor is published only through JitPack, not Maven Central.
        maven("https://jitpack.io")
    }
}

rootProject.name = "youtube-classic"
include(":app")
