package com.vaultgame.core.save

/** Persistence boundary. The real implementation (androidx DataStore + JSON, see
 * app/data/DataStoreGameSaveRepository) lives in the :app module since DataStore is an Android
 * dependency; :core only needs the interface to keep session/economy code testable. */
interface GameSaveRepository {
    suspend fun load(): GameSave
    suspend fun save(save: GameSave)
}
