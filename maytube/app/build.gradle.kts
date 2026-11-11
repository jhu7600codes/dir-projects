plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maytube.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maytube.app"
        // Requested directly: run as far back as the current dependency
        // stack (AndroidX/Media3/Coil) actually allows without ripping any
        // of it out, rather than a separate legacy build/rewrite. 21
        // (Lollipop, 2014) is that real floor -- also roughly where
        // WebView's MSE support (what SABR playback needs) becomes usable
        // at all, so going lower would mean the core playback mechanism
        // itself stops working regardless of what this app's own code
        // does. Every API 24+ call this codebase used before this change
        // is now behind a Build.VERSION.SDK_INT gate with a real fallback
        // for 21-23 (see MainActivity/WatchActivity/PlayerActivity's
        // hideSystemChrome-style methods) rather than a hard floor -- "make
        // it detect which API and use it," not a parallel legacy variant.
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-milestone1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Two genuinely separate APKs from one source tree, not one universal
    // APK that runtime-detects a TV and redirects -- reported directly as
    // "bad concept" after that redirect approach visibly failed to kick in
    // on a real device and showed the touch-oriented WebView UI on a TV
    // screen instead. Splitting into flavors removes that failure mode by
    // construction for the tv flavor: src/tv/AndroidManifest.xml
    // tools:node="remove"s MainActivity (the WebView Activity) outright, so
    // there's no launch-time detection to get wrong -- the WebView simply
    // isn't a reachable component in that APK at all, only HomeActivity's
    // native browse/watch/comments shell is. The mobile flavor is
    // byte-for-byte what this app always was (its own AndroidManifest.xml
    // has no TV-specific declarations at all); DeviceUtils.isTv's
    // redirect logic stays in the shared Kotlin source as a harmless
    // defensive fallback (belt-and-suspenders if the mobile APK ever ends
    // up sideloaded onto a TV box some other way), not as this feature's
    // actual mechanism anymore.
    flavorDimensions += "target"
    productFlavors {
        create("mobile") {
            dimension = "target"
            // BuildConfig.IS_TV_FLAVOR: see HomeActivity's onCreate/
            // settingsLauncher guards. isTv(context) (DeviceUtils.kt,
            // runtime UiModeManager check) is what actually matters for the
            // mobile flavor -- this build still has MainActivity/WebView to
            // fall back to, so a false negative here just means "acts like
            // a phone," not a crash.
            buildConfigField("boolean", "IS_TV_FLAVOR", "false")
        }
        create("tv") {
            dimension = "target"
            applicationIdSuffix = ".tv"
            versionNameSuffix = "-tv"
            // Compile-time-certain counterpart to isTv(context)'s runtime
            // heuristic, specifically for HomeActivity's own guards: this
            // flavor's manifest removes MainActivity outright (see its
            // AndroidManifest.xml kdoc), so anywhere those guards would
            // otherwise navigate to MainActivity has to be unconditionally
            // skipped in this flavor -- not just skipped when isTv(context)
            // happens to return true, since a device that fails that
            // runtime check (the exact bug that motivated this flavor
            // split to begin with) would otherwise crash trying to launch
            // an Activity that isn't a component in this APK at all.
            buildConfigField("boolean", "IS_TV_FLAVOR", "true")
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
