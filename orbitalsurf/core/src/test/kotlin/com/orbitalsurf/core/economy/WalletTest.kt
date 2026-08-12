package com.orbitalsurf.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletTest {
    @Test
    fun `credit increases the balance`() {
        val wallet = Wallet(100).credit(50)
        assertEquals(150L, wallet.plates)
    }

    @Test
    fun `debit within balance succeeds and decreases it`() {
        val wallet = Wallet(100).debit(40)
        assertEquals(60L, wallet!!.plates)
    }

    @Test
    fun `debit beyond balance returns null with no partial effect`() {
        val original = Wallet(30)
        val result = original.debit(100)
        assertNull(result)
        assertEquals(30L, original.plates)
    }

    @Test
    fun `debiting exactly the full balance leaves zero`() {
        val wallet = Wallet(50).debit(50)
        assertEquals(0L, wallet!!.plates)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative credit is rejected`() {
        Wallet(0).credit(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative debit is rejected`() {
        Wallet(0).debit(-1)
    }
}
