package com.orbitalsurf.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopServiceTest {
    @Test
    fun `a successful voucher purchase debits the wallet and increments inventory`() {
        val result = ShopService.purchase(ShopCatalog.missionSkipVoucher, Wallet(1000), Inventory())
        assertTrue(result.success)
        assertEquals(1000 - ShopCatalog.missionSkipVoucher.price, result.wallet.plates)
        assertEquals(1, result.inventory.missionSkipVouchers)
    }

    @Test
    fun `insufficient funds rejects the purchase with zero side effects`() {
        val wallet = Wallet(10)
        val inventory = Inventory()
        val result = ShopService.purchase(ShopCatalog.missionSkipVoucher, wallet, inventory)
        assertFalse(result.success)
        assertEquals(wallet, result.wallet)
        assertEquals(inventory, result.inventory)
    }

    @Test
    fun `buying a second voucher stacks the count`() {
        var wallet = Wallet(1000)
        var inventory = Inventory()
        val first = ShopService.purchase(ShopCatalog.missionSkipVoucher, wallet, inventory)
        wallet = first.wallet; inventory = first.inventory
        val second = ShopService.purchase(ShopCatalog.missionSkipVoucher, wallet, inventory)
        assertEquals(2, second.inventory.missionSkipVouchers)
    }

    @Test
    fun `buying a cosmetic skin the first time succeeds and grants ownership`() {
        val skin = ShopCatalog.cosmeticSkins.first()
        val result = ShopService.purchase(skin, Wallet(10_000), Inventory())
        assertTrue(result.success)
        assertTrue(skin.skinId in result.inventory.ownedSkinIds)
    }

    @Test
    fun `buying an already-owned cosmetic skin again is a rejected no-op, not a double charge`() {
        val skin = ShopCatalog.cosmeticSkins.first()
        val first = ShopService.purchase(skin, Wallet(10_000), Inventory())
        val second = ShopService.purchase(skin, first.wallet, first.inventory)
        assertFalse(second.success)
        assertEquals(first.wallet.plates, second.wallet.plates)
    }

    @Test
    fun `buying a headstart for a locked checkpoint is rejected`() {
        val item = ShopCatalog.headstartFor(3)
        val result = ShopService.purchase(item, Wallet(10_000), Inventory(), unlockedCheckpoints = emptySet())
        assertFalse(result.success)
    }

    @Test
    fun `buying a headstart for an unlocked checkpoint succeeds and grants a ticket`() {
        val item = ShopCatalog.headstartFor(3)
        val result = ShopService.purchase(item, Wallet(10_000), Inventory(), unlockedCheckpoints = setOf(3))
        assertTrue(result.success)
        assertEquals(1, result.inventory.headstartTickets[3])
    }

    @Test
    fun `headstart price grows with checkpoint tier`() {
        val early = ShopCatalog.headstartFor(1)
        val late = ShopCatalog.headstartFor(10)
        assertTrue(late.price > early.price)
    }
}
