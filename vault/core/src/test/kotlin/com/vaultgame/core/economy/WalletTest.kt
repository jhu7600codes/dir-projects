package com.vaultgame.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletTest {
    @Test
    fun creditIncreasesBalance() {
        val wallet = Wallet(100).credit(50)
        assertEquals(150L, wallet.plates)
    }

    @Test
    fun debitWithinBalanceSucceeds() {
        val wallet = Wallet(100).debit(40)
        assertEquals(60L, wallet?.plates)
    }

    @Test
    fun debitBeyondBalanceReturnsNull() {
        assertNull(Wallet(30).debit(40))
    }

    @Test
    fun canAffordReflectsBalance() {
        val wallet = Wallet(100)
        assertTrue(wallet.canAfford(100))
        assertTrue(!wallet.canAfford(101))
    }
}
