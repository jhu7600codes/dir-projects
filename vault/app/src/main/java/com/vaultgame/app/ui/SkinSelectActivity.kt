package com.vaultgame.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaultgame.app.VaultApp
import com.vaultgame.app.data.SkinVisuals
import com.vaultgame.app.databinding.ActivitySkinSelectBinding
import com.vaultgame.core.economy.SkinCatalog
import kotlinx.coroutines.launch

/** Every skin in the catalog: owned ones can be equipped, shop-purchasable ones show their
 * price (buy from the Shop screen), and achievement-only ones stay locked until earned. */
class SkinSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySkinSelectBinding
    private val adapter = ShopAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkinSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.entriesList.layoutManager = LinearLayoutManager(this)
        binding.entriesList.adapter = adapter
        refresh()
    }

    private fun refresh() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val entries = SkinCatalog.all.map { skin ->
                val owned = skin.id in save.inventory.ownedSkins
                val equipped = skin.id == save.inventory.equippedSkin
                // Unowned skins always show LOCKED here regardless of whether they're
                // shop-purchasable or achievement-only -- this screen only equips, buying
                // happens on the Shop screen.
                val status = when {
                    equipped -> ShopEntryStatus.EQUIPPED
                    owned -> ShopEntryStatus.EQUIPPABLE
                    else -> ShopEntryStatus.LOCKED
                }
                ShopEntry(
                    id = skin.id,
                    swatchColor = SkinVisuals.forId(skin.id).jacketAccentColor,
                    name = skin.displayName,
                    description = if (owned || status != ShopEntryStatus.LOCKED) skin.description
                    else "${skin.description} ${if (skin.priceInShop != null) "Buy it in the Shop." else ""}",
                    priceLabel = "",
                    status = status,
                    onClick = { equip(skin.id) },
                )
            }
            adapter.submit(entries)
        }
    }

    private fun equip(skinId: String) {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            repository.save(save.copy(inventory = save.inventory.withSkinEquipped(skinId)))
            refresh()
        }
    }
}
