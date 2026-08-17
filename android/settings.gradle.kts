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
        // libsu (root access helper library, used by :app's root-enhanced popup)
        maven("https://jitpack.io")
        // Classic Xposed API, used by :xposed — see xposed/README.md for why
        // this module targets the classic (de.robv.android.xposed) API
        // surface rather than the newer io.github.libxposed one.
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "androdrop"
include(":app")
// Experimental LSPosed module — see xposed/README.md. Build it on its own
// with `./gradlew :xposed:assembleDebug`; a bare `./gradlew assembleDebug`
// builds both, since Gradle doesn't distinguish "included" from "default".
include(":xposed")
