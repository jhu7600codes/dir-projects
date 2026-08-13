package com.vaultgame.app

import android.app.Application
import com.vaultgame.app.data.DataStoreGameSaveRepository
import com.vaultgame.core.save.GameSaveRepository

class VaultApp : Application() {

    lateinit var gameSaveRepository: GameSaveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        gameSaveRepository = DataStoreGameSaveRepository(this)
    }
}
