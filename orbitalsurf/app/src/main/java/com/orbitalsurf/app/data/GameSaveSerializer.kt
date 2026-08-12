package com.orbitalsurf.app.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.orbitalsurf.core.save.GameSave
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Whole-file JSON (de)serialization for the DataStore behind [DataStoreGameSaveRepository].
 * Reuses exactly the JSON logic exercised by `:core`'s `GameSaveSerializationTest` -- this
 * file is deliberately thin, just an `InputStream`/`OutputStream` adapter around
 * `kotlinx.serialization`, so there's nothing here for that test to not have already covered.
 */
object GameSaveSerializer : Serializer<GameSave> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: GameSave = GameSave()

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
