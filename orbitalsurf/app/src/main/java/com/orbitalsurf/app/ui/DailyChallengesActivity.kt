package com.orbitalsurf.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbitalsurf.app.OrbitalSurfApp
import com.orbitalsurf.app.databinding.ActivityDailyChallengesBinding
import com.orbitalsurf.core.session.RunResultApplier
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Today's 1-3 daily challenges -- harder than the rolling missions, evaluated once per finished
 * run (see `RunResultApplier`/`GameActivity.finishRun`), not incrementally. This screen only
 * displays state; actually completing a challenge always happens by finishing a run.
 */
class DailyChallengesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyChallengesBinding
    private lateinit var adapter: DailyChallengesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = DailyChallengesAdapter()
        binding.dailyChallengesList.layoutManager = LinearLayoutManager(this)
        binding.dailyChallengesList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val todayEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
            val dailySystem = RunResultApplier.dailyChallengeSystemFrom(save)
            dailySystem.ensureUpToDate(todayEpochDay)
            adapter.submitList(dailySystem.challenges)
        }
    }
}
