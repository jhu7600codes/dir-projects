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

rootProject.name = "vanbank"

// :core is a plain Kotlin/JVM module (no Android dependency) and is always
// included -- it holds every piece of VANBank logic that doesn't need an
// Android runtime: password hashing, DIR card/account number generation
// (with a real Luhn checksum), loan amortization math, statement totals,
// and budget category aggregation. It's the part of this project that can
// actually be compiled and unit-tested in environments without an Android
// SDK installed.
include(":core")

// :app is the real Android application module: Compose UI, Room database,
// WorkManager bill scheduling, notifications. It depends on :core and
// requires a working Android SDK (ANDROID_HOME / local.properties) to
// configure at all -- AGP resolves the SDK during Gradle's configuration
// phase, before any task even runs. If you're working in an environment
// without the SDK set up and only need to build/test :core, comment out
// the line below; ./gradlew :core:test keeps working either way thanks to
// org.gradle.configureondemand=true in gradle.properties.
include(":app")
