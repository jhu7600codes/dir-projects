plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.androdrop.xposed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.androdrop.xposed"
        minSdk = 27 // LSPosed's own minimum
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // This module doesn't run standalone — it's loaded into other
    // processes' address space by LSPosed itself, so its own "app" never
    // launches on its own.
    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("androidx.core:core-ktx:1.13.1")
}
