package com.vaultgame.core.save

/** Test/preview double -- no disk I/O, just holds the latest save in memory. */
class InMemoryGameSaveRepository(initial: GameSave) : GameSaveRepository {
    private var current: GameSave = initial

    override suspend fun load(): GameSave = current

    override suspend fun save(save: GameSave) {
        current = save
    }
}
