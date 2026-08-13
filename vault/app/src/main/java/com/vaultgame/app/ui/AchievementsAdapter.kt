package com.vaultgame.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaultgame.app.R
import com.vaultgame.app.databinding.ItemAchievementBinding

data class AchievementEntry(val name: String, val description: String, val unlocked: Boolean)

class AchievementsAdapter(private var entries: List<AchievementEntry> = emptyList()) :
    RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {

    fun submit(newEntries: List<AchievementEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(entries[position])

    override fun getItemCount(): Int = entries.size

    class ViewHolder(private val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: AchievementEntry) {
            val context = binding.root.context
            binding.nameText.text = entry.name
            binding.descriptionText.text = entry.description
            binding.statusText.text =
                context.getString(if (entry.unlocked) R.string.achievement_unlocked else R.string.achievement_locked)
            binding.statusText.setTextColor(
                context.getColor(if (entry.unlocked) R.color.vault_success else R.color.vault_on_surface_muted),
            )
            binding.root.alpha = if (entry.unlocked) 1f else 0.6f
        }
    }
}
