package com.orbitalsurf.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orbitalsurf.app.databinding.ItemAchievementBinding
import com.orbitalsurf.core.progression.Achievement

/** Precomputed display state for one achievement row. */
data class AchievementRow(
    val achievement: Achievement,
    val unlocked: Boolean,
    val progressText: String,
    val actionText: String,
    val actionEnabled: Boolean,
    val isExternalLink: Boolean,
)

class AchievementsAdapter(
    private val onOpenLink: (Achievement.ExternalLinkAchievement) -> Unit,
    private val onEquip: (skinId: String) -> Unit,
) : RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {
    private var rows: List<AchievementRow> = emptyList()

    fun submitList(newRows: List<AchievementRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], onOpenLink, onEquip)
    }

    override fun getItemCount(): Int = rows.size

    class ViewHolder(private val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            row: AchievementRow,
            onOpenLink: (Achievement.ExternalLinkAchievement) -> Unit,
            onEquip: (String) -> Unit,
        ) {
            binding.achievementLabel.text = row.achievement.label
            binding.achievementProgress.text = row.progressText
            binding.actionButton.text = row.actionText
            binding.actionButton.isEnabled = row.actionEnabled
            binding.actionButton.setOnClickListener {
                if (row.isExternalLink && !row.unlocked) {
                    onOpenLink(row.achievement as Achievement.ExternalLinkAchievement)
                } else {
                    onEquip(row.achievement.rewardSkinId)
                }
            }
        }
    }
}
