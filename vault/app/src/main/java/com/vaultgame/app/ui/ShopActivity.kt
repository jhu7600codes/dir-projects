package com.vaultgame.app.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.data.SkinVisuals
import com.vaultgame.app.databinding.ActivityShopBinding
import com.vaultgame.core.economy.ShopCatalog
import com.vaultgame.core.economy.ShopItem
import com.vaultgame.core.economy.ShopService
import com.vaultgame.core.save.GameSave
import kotlinx.coroutines.launch

/** Cosmetics-and-convenience-only shop, tabbed by category. Every purchase re-reads/re-writes
 * the save so the balance/ownership shown always matches what's persisted. */
class ShopActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShopBinding
    private val adapter = ShopAdapter()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.entriesList.layoutManager = LinearLayoutManager(this)
        binding.entriesList.adapter = adapter

        for (label in listOf(
            getString(R.string.shop_tab_skins), getString(R.string.shop_tab_powerups),
            getString(R.string.shop_tab_vouchers), getString(R.string.shop_tab_headstarts),
        )) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(label))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                refresh()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        refresh()
    }

    private fun refresh() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            binding.platesText.text = getString(R.string.shop_price_format, save.wallet.plates.toInt())
            adapter.submit(buildEntries(currentTab, save))
        }
    }

    private fun buildEntries(tab: Int, save: GameSave): List<ShopEntry> = when (tab) {
        0 -> ShopCatalog.skins().map { skinEntry(it, save) }
        1 -> ShopCatalog.powerupUpgrades().map { powerupEntry(it, save) }
        2 -> listOf(voucherEntry(save))
        else -> ShopCatalog.headstarts().map { headstartEntry(it, save) }
    }

    private fun skinEntry(item: ShopItem.Skin, save: GameSave): ShopEntry {
        val owned = item.definition.id in save.inventory.ownedSkins
        return ShopEntry(
            id = item.id,
            swatchColor = SkinVisuals.forId(item.definition.id).jacketAccentColor,
            name = item.name,
            description = item.description,
            priceLabel = getString(R.string.shop_price_format, item.price.toInt()),
            status = when {
                owned -> ShopEntryStatus.OWNED
                save.wallet.canAfford(item.price) -> ShopEntryStatus.BUYABLE
                else -> ShopEntryStatus.UNAFFORDABLE
            },
            onClick = { purchase(item) },
        )
    }

    private fun powerupEntry(item: ShopItem.PowerupUpgrade, save: GameSave): ShopEntry {
        val owned = save.inventory.upgradeLevel(item.powerupType) >= item.level
        val locked = save.inventory.upgradeLevel(item.powerupType) < item.level - 1
        return ShopEntry(
            id = item.id,
            swatchColor = Color.parseColor("#7A6BC9"),
            name = item.name,
            description = item.description,
            priceLabel = getString(R.string.shop_price_format, item.price.toInt()),
            status = when {
                owned -> ShopEntryStatus.OWNED
                locked -> ShopEntryStatus.LOCKED
                save.wallet.canAfford(item.price) -> ShopEntryStatus.BUYABLE
                else -> ShopEntryStatus.UNAFFORDABLE
            },
            onClick = { purchase(item) },
        )
    }

    private fun voucherEntry(save: GameSave): ShopEntry {
        val item = ShopItem.MissionSkipVoucher
        return ShopEntry(
            id = item.id,
            swatchColor = Color.parseColor("#4CC9A0"),
            name = "${item.name} (${save.inventory.missionSkipVouchers} owned)",
            description = item.description,
            priceLabel = getString(R.string.shop_price_format, item.price.toInt()),
            status = if (save.wallet.canAfford(item.price)) ShopEntryStatus.BUYABLE else ShopEntryStatus.UNAFFORDABLE,
            onClick = { purchase(item) },
        )
    }

    private fun headstartEntry(item: ShopItem.Headstart, save: GameSave): ShopEntry {
        val owned = item.checkpointDistanceMeters in save.inventory.unlockedHeadstarts
        return ShopEntry(
            id = item.id,
            swatchColor = Color.parseColor("#5FA8D3"),
            name = item.name,
            description = item.description,
            priceLabel = getString(R.string.shop_price_format, item.price.toInt()),
            status = when {
                owned -> ShopEntryStatus.OWNED
                save.wallet.canAfford(item.price) -> ShopEntryStatus.BUYABLE
                else -> ShopEntryStatus.UNAFFORDABLE
            },
            onClick = { purchase(item) },
        )
    }

    private fun purchase(item: ShopItem) {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            when (val result = ShopService.purchase(item, save.wallet, save.inventory)) {
                is ShopService.PurchaseResult.Success -> {
                    repository.save(save.copy(wallet = result.wallet, inventory = result.inventory))
                    refresh()
                }
                ShopService.PurchaseResult.InsufficientFunds ->
                    Toast.makeText(this@ShopActivity, R.string.shop_insufficient_funds, Toast.LENGTH_SHORT).show()
                ShopService.PurchaseResult.AlreadyOwned ->
                    Toast.makeText(this@ShopActivity, R.string.shop_owned, Toast.LENGTH_SHORT).show()
                ShopService.PurchaseResult.SkipsPrerequisiteLevel ->
                    Toast.makeText(this@ShopActivity, R.string.shop_locked_prerequisite, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
