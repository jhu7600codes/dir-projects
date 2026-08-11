package com.maytube.app

import android.app.Application
import android.webkit.WebView
import com.maytube.app.data.ServerConfigRepository

class MaytubeApp : Application() {

    lateinit var serverConfigRepository: ServerConfigRepository
        private set

    override fun onCreate() {
        super.onCreate()
        serverConfigRepository = ServerConfigRepository(this)

        // lets a PC on the same network (or over USB) inspect this app's
        // WebView via chrome://inspect -- real console/network output for
        // debugging things like SABR/MSE playback instead of guessing from
        // screenshots. Must be called before any WebView is created, and
        // only in debug builds.
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
