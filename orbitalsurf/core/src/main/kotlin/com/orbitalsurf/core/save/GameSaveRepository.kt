package com.orbitalsurf.core.save

/** Persistence boundary. `:app` provides a DataStore-backed implementation; `:core` only needs this interface. */
interface GameSaveRepository {
    suspend fun load(): GameSave
    suspend fun save(save: GameSave)
}
