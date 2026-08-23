plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ytclassic.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ytclassic.app"
        // NewPipeExtractor requires core library desugaring (java.time / nio)
        // below API 33; minSdk 24 covers everything else this app needs
        // (Media3 background playback, foreground services for downloads).
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        // Custom playback (playback/PlaybackService.kt's merged video-only +
        // audio-only MediaSource.Factory) necessarily reaches into Media3
        // APIs marked @UnstableApi - opt in at the module level instead of
        // annotating every call site up the chain, same as Media3's own
        // sample apps do.
        freeCompilerArgs += "-opt-in=androidx.media3.common.util.UnstableApi"
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        // Kotlin's own -opt-in flag above already makes the @UnstableApi
        // usage (the custom merged video-only/audio-only MediaSource.Factory
        // in playback/, and everything that has to reference it - the
        // player screen, download/playback intents, etc.) a deliberate,
        // module-wide decision rather than something each call site needs
        // to re-declare. Lint's opt-in detector doesn't read that compiler
        // flag, so without this it would flag every one of those call
        // sites as if the opt-in were accidental.
        disable += "UnsafeOptInUsageError"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- Extraction backend: no official YouTube Data API key, no OAuth ---
    // Search / video info / stream URLs / comments all come from scraping +
    // reverse-engineered internal endpoints, wrapped by NewPipeExtractor.
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.26.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Thumbnails / avatars.
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- Playback: Media3/ExoPlayer, with a MediaSessionService for
    // background + offline (downloaded file) playback and PiP-friendly
    // controls. ---
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
