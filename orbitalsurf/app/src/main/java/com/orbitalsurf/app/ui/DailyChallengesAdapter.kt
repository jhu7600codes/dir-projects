package com.orbitalsurf.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orbitalsurf.app.R
import com.orbitalsurf.app.databinding.ItemDailyChallengeBinding
import com.orbitalsurf.core.progression.DailyChallenge

class DailyChallengesAdapter : RecyclerView.Adapter<DailyChallengesAdapter.ViewHolder>() {
    private var challenges: List<DailyChallenge> = emptyList()

    fun submitList(newChallenges: List<DailyChallenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(challenges[position])
    }

    override fun getItemCount(): Int = challenges.size

    class ViewHolder(private val binding: ItemDailyChallengeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(challenge: DailyChallenge) {
            binding.challengeDescription.text = challenge.description
            val context = binding.root.context
            binding.challengeReward.text = if (challenge.completed) {
                context.getString(R.string.daily_challenge_completed)
            } else {
                context.getString(R.string.daily_challenge_reward_format, challenge.plateReward)
            }
            binding.challengeDescription.alpha = if (challenge.completed) 0.5f else 1f
        }
    }
}
