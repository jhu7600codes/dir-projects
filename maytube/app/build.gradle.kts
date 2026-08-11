plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maytube.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maytube.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-milestone1"

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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // used by the fast SABR-fragment downloader (concurrent fetch + remux),
    // see download/SabrFragmentDownloader.kt
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // yt2009 has no JSON API -- it's server-rendered HTML, so the native
    // (non-WebView) browse/search/watch/comments screens parse its actual
    // page markup directly. See browse/Yt2009Api.kt for the real
    // selectors, read from yt2009's own source rather than guessed.
    implementation("org.jsoup:jsoup:1.17.2")

    // true live-streaming native playback (see player/StreamingPlayer.kt):
    // feeds ExoPlayer from the same SABR fragments the WebView/downloader
    // pipeline uses, as they arrive, instead of buffering a whole video to
    // a finished local file first like the original VideoView-based
    // PlayerActivity did.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // thumbnail/avatar loading for the native browse/watch/comments screens
    implementation("io.coil-kt:coil:2.6.0")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json classes are unimplemented stubs on the plain JVM
    // unit test classpath; this real implementation shadows them so code
    // under test (MobileInjector uses org.json.JSONObject) actually works.
    testImplementation("org.json:json:20240303")
}
