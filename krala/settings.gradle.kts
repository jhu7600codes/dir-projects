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
        // requery's sqlite build ships fts5 on every android version (the system sqlite does not)
        maven("https://jitpack.io") { content { includeGroup("com.github.requery") } }
    }
}

rootProject.name = "krala"
include(":app")
