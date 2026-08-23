plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Must match the actual Kotlin package layout (com.msfviewer, not
    // com.msfviewer.app) -- the manifest's relative ".MainActivity" name
    // resolves against this at build time, and a mismatch here silently
    // packages fine but crashes at launch with ClassNotFoundException.
    namespace = "com.msfviewer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.msfviewer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Compatible with Kotlin 1.9.24 -- see
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Parser/layout engine unit tests -- pure Kotlin, no Android framework
    // dependency, so these run as fast local JVM tests.
    testImplementation("junit:junit:4.13.2")
}
