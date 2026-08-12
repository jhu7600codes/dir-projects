package com.orbitalsurf.app

import android.app.Application
import com.orbitalsurf.app.data.DataStoreGameSaveRepository
import com.orbitalsurf.core.save.GameSaveRepository

class OrbitalSurfApp : Application() {

    lateinit var gameSaveRepository: GameSaveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        gameSaveRepository = DataStoreGameSaveRepository(this)
    }
}
