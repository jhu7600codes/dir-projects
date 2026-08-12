package com.orbitalsurf.core.save

/** A trivial in-memory implementation -- useful in `:core`'s own tests, and reusable later as `:app`'s test fake. */
class InMemoryGameSaveRepository(initial: GameSave = GameSaveDefaults.new()) : GameSaveRepository {
    private var current: GameSave = initial

    override suspend fun load(): GameSave = current

    override suspend fun save(save: GameSave) {
        current = save
    }
}
