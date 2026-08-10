package com.maytube.app

import android.app.Application
import com.maytube.app.data.ServerConfigRepository

class MaytubeApp : Application() {

    lateinit var serverConfigRepository: ServerConfigRepository
        private set

    override fun onCreate() {
        super.onCreate()
        serverConfigRepository = ServerConfigRepository(this)
    }
}
