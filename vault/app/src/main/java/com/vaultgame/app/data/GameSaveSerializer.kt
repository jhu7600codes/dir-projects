package com.vaultgame.app.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.vaultgame.core.save.GameSave
import com.vaultgame.core.save.GameSaveDefaults
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Whole-file JSON (de)serialization for the DataStore behind [DataStoreGameSaveRepository].
 * Reuses exactly the JSON logic exercised by :core's GameSaveSerializationTest -- this file is
 * deliberately thin, just an InputStream/OutputStream adapter around kotlinx.serialization.
 *
 * [defaultValue] uses worldSeed 0 as a sentinel for "no save exists yet" -- a real random seed
 * can't be picked here (no I/O, must stay synchronous), so [DataStoreGameSaveRepository.load]
 * re-seeds and persists a proper save the first time it sees worldSeed 0.
 */
object GameSaveSerializer : Serializer<GameSave> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val UNINITIALIZED_SEED = 0L

    override val defaultValue: GameSave = GameSaveDefaults.new(worldSeed = UNINITIALIZED_SEED)

    override suspend fun readFrom(input: InputStream): GameSave {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try {
            json.decodeFromString(text)
        } catch (e: SerializationException) {
            throw CorruptionException("Could not parse GameSave JSON", e)
        }
    }

    override suspend fun writeTo(t: GameSave, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
