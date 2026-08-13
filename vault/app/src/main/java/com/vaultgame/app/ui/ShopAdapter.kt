package com.vaultgame.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaultgame.app.R
import com.vaultgame.app.databinding.ItemShopEntryBinding

enum class ShopEntryStatus { BUYABLE, UNAFFORDABLE, OWNED, LOCKED, EQUIPPABLE, EQUIPPED }

data class ShopEntry(
    val id: String,
    val swatchColor: Int,
    val name: String,
    val description: String,
    val priceLabel: String,
    val status: ShopEntryStatus,
    val onClick: () -> Unit,
)

class ShopAdapter(private var entries: List<ShopEntry> = emptyList()) : RecyclerView.Adapter<ShopAdapter.ViewHolder>() {

    fun submit(newEntries: List<ShopEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShopEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(entries[position])

    override fun getItemCount(): Int = entries.size

    class ViewHolder(private val binding: ItemShopEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: ShopEntry) {
            val context = binding.root.context
            binding.swatch.background?.setTint(entry.swatchColor)
            binding.nameText.text = entry.name
            binding.descriptionText.text = entry.description
            binding.actionButton.isEnabled = entry.status == ShopEntryStatus.BUYABLE || entry.status == ShopEntryStatus.EQUIPPABLE
            binding.actionButton.text = when (entry.status) {
                ShopEntryStatus.BUYABLE -> entry.priceLabel
                ShopEntryStatus.UNAFFORDABLE -> entry.priceLabel
                ShopEntryStatus.OWNED -> context.getString(R.string.shop_owned)
                ShopEntryStatus.LOCKED -> context.getString(R.string.shop_locked_prerequisite)
                ShopEntryStatus.EQUIPPABLE -> context.getString(R.string.shop_equip)
                ShopEntryStatus.EQUIPPED -> context.getString(R.string.shop_equipped)
            }
            binding.actionButton.setOnClickListener {
                if (entry.status == ShopEntryStatus.BUYABLE || entry.status == ShopEntryStatus.EQUIPPABLE) entry.onClick()
            }
        }
    }
}
