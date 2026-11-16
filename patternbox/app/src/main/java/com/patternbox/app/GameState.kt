package com.patternbox.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.patternbox.core.Loadout

/** the two fighters being built, saved as share codes so they survive restarts */
class GameState(context: Context) {
    private val prefs = context.getSharedPreferences("patternbox", Context.MODE_PRIVATE)

    val loadouts = mutableStateListOf(
        load(0) ?: Loadout.DEFAULT_P1,
        load(1) ?: Loadout.DEFAULT_P2,
    )
    val names = mutableStateListOf(
        prefs.getString("name0", null) ?: loadouts[0].autoName,
        prefs.getString("name1", null) ?: loadouts[1].autoName,
    )

    private fun load(i: Int) = prefs.getString("code$i", null)?.let { Loadout.decode(it) }

    fun set(i: Int, l: Loadout) {
        loadouts[i] = l
        save()
    }

    fun rename(i: Int, n: String) {
        names[i] = n
        save()
    }

    fun nameFor(i: Int) = names[i].ifBlank { loadouts[i].autoName }

    private fun save() {
        prefs.edit()
            .putString("code0", loadouts[0].encode()).putString("code1", loadouts[1].encode())
            .putString("name0", names[0]).putString("name1", names[1])
            .apply()
    }
}
