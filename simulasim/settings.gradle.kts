pluginManagement {
    repositories {
        google()
        mavenCentral()
        // google's mirror of central, for when central rate-limits
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // google's mirror of central, for when central rate-limits
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

rootProject.name = "simulasim"

include(":app")
