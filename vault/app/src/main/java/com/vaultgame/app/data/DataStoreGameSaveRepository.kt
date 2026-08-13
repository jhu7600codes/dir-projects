package com.vaultgame.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.vaultgame.core.save.GameSave
import com.vaultgame.core.save.GameSaveDefaults
import com.vaultgame.core.save.GameSaveRepository
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.random.Random

/**
 * DataStore-backed implementation of :core's [GameSaveRepository] -- one JSON file at
 * files/datastore/game_save.json (see res/xml/backup_rules.xml / data_extraction_rules.xml,
 * which back up exactly that path).
 */
class DataStoreGameSaveRepository(context: Context) : GameSaveRepository {
    private val dataStore: DataStore<GameSave> = DataStoreFactory.create(
        serializer = GameSaveSerializer,
        produceFile = {
            val dir = File(context.filesDir, "datastore")
            if (!dir.exists()) dir.mkdirs()
            File(dir, FILE_NAME)
        },
    )

    override suspend fun load(): GameSave {
        val save = dataStore.data.first()
        if (save.worldSeed != GameSaveSerializer.UNINITIALIZED_SEED) return save

        // First ever load: mint a real world seed and persist it immediately so every later
        // load (and every procedural run) is deterministic from here on.
        var seed = Random.nextLong()
        if (seed == GameSaveSerializer.UNINITIALIZED_SEED) seed = 1L
        val fresh = GameSaveDefaults.new(worldSeed = seed)
        dataStore.updateData { fresh }
        return fresh
    }

    override suspend fun save(save: GameSave) {
        dataStore.updateData { save }
    }

    private companion object {
        const val FILE_NAME = "game_save.json"
    }
}
