package com.orbitalsurf.core.save

import com.orbitalsurf.core.economy.Wallet
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSaveRepositoryContractTest {
    @Test
    fun `a fresh repository loads the default save`() = runTest {
        val repository: GameSaveRepository = InMemoryGameSaveRepository()
        assertEquals(GameSaveDefaults.new(), repository.load())
    }

    @Test
    fun `load-mutate-save-reload round trips exactly`() = runTest {
        val repository: GameSaveRepository = InMemoryGameSaveRepository()
        val loaded = repository.load()
        val mutated = loaded.copy(bestScore = 4_200L, wallet = Wallet(plates = 999))

        repository.save(mutated)
        val reloaded = repository.load()

        assertEquals(mutated, reloaded)
    }

    @Test
    fun `a repository constructed with an initial save starts from that save`() = runTest {
        val initial = GameSaveDefaults.new().copy(bestScore = 77L)
        val repository: GameSaveRepository = InMemoryGameSaveRepository(initial)
        assertEquals(initial, repository.load())
    }
}
