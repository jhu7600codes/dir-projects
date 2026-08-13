package com.vaultgame.core.economy

import com.vaultgame.core.powerups.PowerupType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopServiceTest {
    @Test
    fun purchaseSkinSucceedsAndDebitsWallet() {
        val skin = ShopCatalog.skins().first()
        val wallet = Wallet(skin.price + 100)
        val result = ShopService.purchase(skin, wallet, Inventory())
        assertTrue(result is ShopService.PurchaseResult.Success)
        result as ShopService.PurchaseResult.Success
        assertEquals(100L, result.wallet.plates)
        assertTrue(skin.definition.id in result.inventory.ownedSkins)
    }

    @Test
    fun insufficientFundsIsRejected() {
        val skin = ShopCatalog.skins().first()
        val result = ShopService.purchase(skin, Wallet(0), Inventory())
        assertEquals(ShopService.PurchaseResult.InsufficientFunds, result)
    }

    @Test
    fun alreadyOwnedSkinIsRejected() {
        val skin = ShopCatalog.skins().first()
        val inventory = Inventory(ownedSkins = setOf(SkinCatalog.DEFAULT_SKIN_ID, skin.definition.id))
        val result = ShopService.purchase(skin, Wallet(999_999), inventory)
        assertEquals(ShopService.PurchaseResult.AlreadyOwned, result)
    }

    @Test
    fun powerupUpgradesMustBeBoughtInOrder() {
        val level2 = ShopCatalog.powerupUpgrades().first { it.powerupType == PowerupType.MAGNET && it.level == 2 }
        val result = ShopService.purchase(level2, Wallet(999_999), Inventory())
        assertEquals(ShopService.PurchaseResult.SkipsPrerequisiteLevel, result)
    }

    @Test
    fun powerupUpgradesApplyInOrder() {
        val level1 = ShopCatalog.powerupUpgrades().first { it.powerupType == PowerupType.MAGNET && it.level == 1 }
        val afterLevel1 = ShopService.purchase(level1, Wallet(999_999), Inventory()) as ShopService.PurchaseResult.Success
        assertEquals(1, afterLevel1.inventory.upgradeLevel(PowerupType.MAGNET))

        val level2 = ShopCatalog.powerupUpgrades().first { it.powerupType == PowerupType.MAGNET && it.level == 2 }
        val afterLevel2 = ShopService.purchase(level2, afterLevel1.wallet, afterLevel1.inventory) as ShopService.PurchaseResult.Success
        assertEquals(2, afterLevel2.inventory.upgradeLevel(PowerupType.MAGNET))
    }

    @Test
    fun missionSkipVoucherIsStackable() {
        val voucher = ShopItem.MissionSkipVoucher
        val first = ShopService.purchase(voucher, Wallet(999_999), Inventory()) as ShopService.PurchaseResult.Success
        assertEquals(1, first.inventory.missionSkipVouchers)
        val second = ShopService.purchase(voucher, first.wallet, first.inventory) as ShopService.PurchaseResult.Success
        assertEquals(2, second.inventory.missionSkipVouchers)
    }

    @Test
    fun headstartUnlocksOncePerCheckpoint() {
        val headstart = ShopCatalog.headstarts().first()
        val bought = ShopService.purchase(headstart, Wallet(999_999), Inventory()) as ShopService.PurchaseResult.Success
        assertTrue(headstart.checkpointDistanceMeters in bought.inventory.unlockedHeadstarts)

        val repeatPurchase = ShopService.purchase(headstart, bought.wallet, bought.inventory)
        assertEquals(ShopService.PurchaseResult.AlreadyOwned, repeatPurchase)
    }

    @Test
    fun redeemVoucherDecrementsCountAndFailsAtZero() {
        val withVoucher = Inventory(missionSkipVouchers = 1)
        val afterRedeem = ShopService.redeemMissionSkipVoucher(withVoucher)
        assertEquals(0, afterRedeem?.missionSkipVouchers)
        assertEquals(null, ShopService.redeemMissionSkipVoucher(Inventory(missionSkipVouchers = 0)))
    }
}
