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

rootProject.name = "orbitalsurf"

// :core is a plain Kotlin/JVM module (no Android dependency) and is always
// included -- it's the part of this project that can actually be compiled
// and unit-tested in environments without an Android SDK installed.
include(":core")

// :app is the real Android application module. It depends on :core and
// requires a working Android SDK (ANDROID_HOME / local.properties) to
// configure at all -- AGP resolves the SDK during Gradle's configuration
// phase, before any task even runs. If you're working in an environment
// without the SDK set up and only need to build/test :core, comment out
// the line below; ./gradlew :core:test will keep working either way
// thanks to org.gradle.configureondemand=true in gradle.properties.
include(":app")
