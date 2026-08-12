package com.orbitalsurf.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orbitalsurf.app.databinding.ItemShopEntryBinding
import com.orbitalsurf.core.economy.ShopItem

/** Precomputed display state for one shop row -- the activity derives this from `GameSave` so the adapter itself stays state-free. */
data class ShopRow(
    val item: ShopItem,
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val buttonEnabled: Boolean,
)

class ShopAdapter(private val onTapButton: (ShopItem) -> Unit) : RecyclerView.Adapter<ShopAdapter.ViewHolder>() {
    private var rows: List<ShopRow> = emptyList()

    fun submitList(newRows: List<ShopRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShopEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], onTapButton)
    }

    override fun getItemCount(): Int = rows.size

    class ViewHolder(private val binding: ItemShopEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ShopRow, onTapButton: (ShopItem) -> Unit) {
            binding.itemTitle.text = row.title
            binding.itemSubtitle.text = row.subtitle
            binding.buyButton.text = row.buttonText
            binding.buyButton.isEnabled = row.buttonEnabled
            binding.buyButton.setOnClickListener { onTapButton(row.item) }
        }
    }
}
