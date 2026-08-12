package com.orbitalsurf.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.orbitalsurf.core.save.GameSave
import com.orbitalsurf.core.save.GameSaveRepository
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * DataStore-backed implementation of `:core`'s [GameSaveRepository] -- one JSON file at
 * `files/datastore/game_save.json` (see `res/xml/backup_rules.xml`/`data_extraction_rules.xml`,
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

    override suspend fun load(): GameSave = dataStore.data.first()

    override suspend fun save(save: GameSave) {
        dataStore.updateData { save }
    }

    private companion object {
        const val FILE_NAME = "game_save.json"
    }
}
