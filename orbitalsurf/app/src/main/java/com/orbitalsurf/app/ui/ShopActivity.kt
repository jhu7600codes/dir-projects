package com.orbitalsurf.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbitalsurf.app.OrbitalSurfApp
import com.orbitalsurf.app.R
import com.orbitalsurf.app.data.SkinCatalog
import com.orbitalsurf.app.databinding.ActivityShopBinding
import com.orbitalsurf.core.economy.ShopCatalog
import com.orbitalsurf.core.economy.ShopItem
import com.orbitalsurf.core.economy.ShopService
import com.orbitalsurf.core.save.GameSave
import kotlinx.coroutines.launch

/** Mission Skip Vouchers, unlocked-checkpoint Headstarts, and cosmetic skins -- all bought (and skins equipped) with plates. */
class ShopActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShopBinding
    private lateinit var adapter: ShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ShopAdapter { item -> onTapButton(item) }
        binding.shopList.layoutManager = LinearLayoutManager(this)
        binding.shopList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            binding.platesText.text = getString(R.string.menu_plates_format, save.wallet.plates)
            adapter.submitList(ShopCatalog.all(save.checkpointUnlocks.unlocked).map { rowFor(it, save) })
        }
    }

    private fun rowFor(item: ShopItem, save: GameSave): ShopRow = when (item) {
        is ShopItem.MissionSkipVoucherItem -> ShopRow(
            item = item,
            title = getString(R.string.shop_section_vouchers),
            subtitle = "Owned: ${save.inventory.missionSkipVouchers}",
            buttonText = getString(R.string.shop_price_format, item.price),
            buttonEnabled = save.wallet.plates >= item.price,
        )
        is ShopItem.HeadstartItem -> ShopRow(
            item = item,
            title = getString(R.string.shop_headstart_item_format, item.checkpointIndex),
            subtitle = "Owned: ${save.inventory.headstartTickets[item.checkpointIndex] ?: 0}",
            buttonText = getString(R.string.shop_price_format, item.price),
            buttonEnabled = save.wallet.plates >= item.price,
        )
        is ShopItem.CosmeticSkinItem -> {
            val owned = item.skinId in save.inventory.ownedSkinIds
            val equipped = save.inventory.equippedSkinId == item.skinId
            ShopRow(
                item = item,
                title = SkinCatalog.forId(item.skinId).displayName,
                subtitle = when {
                    equipped -> getString(R.string.achievement_unlocked) + " • Equipped"
                    owned -> getString(R.string.shop_owned)
                    else -> ""
                },
                buttonText = when {
                    equipped -> "Equipped"
                    owned -> "Equip"
                    else -> getString(R.string.shop_price_format, item.price)
                },
                buttonEnabled = !equipped && (owned || save.wallet.plates >= item.price),
            )
        }
    }

    private fun onTapButton(item: ShopItem) {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()

            if (item is ShopItem.CosmeticSkinItem && item.skinId in save.inventory.ownedSkinIds) {
                repository.save(save.copy(inventory = save.inventory.copy(equippedSkinId = item.skinId)))
                refresh()
                return@launch
            }

            val result = ShopService.purchase(item, save.wallet, save.inventory, save.checkpointUnlocks.unlocked)
            if (!result.success) {
                Toast.makeText(this@ShopActivity, R.string.shop_insufficient_plates, Toast.LENGTH_SHORT).show()
                return@launch
            }
            repository.save(save.copy(wallet = result.wallet, inventory = result.inventory))
            refresh()
        }
    }
}
