package com.patternbox.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patternbox.core.Entry
import com.patternbox.core.Loadout
import com.patternbox.core.Presets

data class RosterFighter(
    val id: Long,
    val name: String,
    val loadout: Loadout,
    val wins: Int = 0,
    val losses: Int = 0,
    val kos: Int = 0,
    val cups: Int = 0,
) {
    val displayName get() = name.ifBlank { loadout.autoName }
    fun entry() = Entry(displayName, loadout, id)
}

enum class ThemeMode(val label: String) { SYSTEM("system"), LIGHT("light"), DARK("dark") }

/** everything that gets saved: the roster, records and settings */
class GameState(context: Context) {
    private val prefs = context.getSharedPreferences("patternbox", Context.MODE_PRIVATE)

    val roster = mutableStateListOf<RosterFighter>()

    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true)); private set
    var damageNumbers by mutableStateOf(prefs.getBoolean("dmgnums", true)); private set
    var screenShake by mutableStateOf(prefs.getBoolean("shake", true)); private set
    var dynamicColor by mutableStateOf(prefs.getBoolean("dyncolor", true)); private set
    var themeMode by mutableStateOf(ThemeMode.values().getOrElse(prefs.getInt("theme", 0)) { ThemeMode.SYSTEM }); private set
    var gameSpeed by mutableStateOf(prefs.getFloat("speed", 1f)); private set
    var bestWave by mutableIntStateOf(prefs.getInt("bestwave", 0)); private set
    var totalKos by mutableIntStateOf(prefs.getInt("totalkos", 0)); private set
    var matchesPlayed by mutableIntStateOf(prefs.getInt("matches", 0)); private set

    private var idCounter = prefs.getLong("idcounter", 1L)
    private fun newId(): Long {
        idCounter++
        prefs.edit().putLong("idcounter", idCounter).apply()
        return idCounter
    }

    init {
        load()
        if (roster.isEmpty()) {
            // first launch (or upgrade from 0.1 which saved two codes)
            val old0 = prefs.getString("code0", null)?.let { Loadout.decode(it) }
            val old1 = prefs.getString("code1", null)?.let { Loadout.decode(it) }
            roster += RosterFighter(newId(), prefs.getString("name0", null) ?: "", old0 ?: Loadout.DEFAULT_P1)
            roster += RosterFighter(newId(), prefs.getString("name1", null) ?: "", old1 ?: Loadout.DEFAULT_P2)
            for ((n, l) in Presets.all.take(4)) roster += RosterFighter(newId(), n, l)
            save()
        }
    }

    fun byId(id: Long) = roster.firstOrNull { it.id == id }

    fun add(name: String, l: Loadout): RosterFighter {
        val f = RosterFighter(newId(), name, l)
        roster.add(0, f)
        save()
        return f
    }

    fun update(f: RosterFighter) {
        val i = roster.indexOfFirst { it.id == f.id }
        if (i >= 0) roster[i] = f else roster.add(0, f)
        save()
    }

    fun delete(id: Long) {
        roster.removeAll { it.id == id }
        save()
    }

    fun duplicate(id: Long): RosterFighter? {
        val f = byId(id) ?: return null
        return add(f.displayName + " 2", f.loadout)
    }

    /** match results: who won, who lost, kos per roster fighter */
    fun record(winners: Set<Long>, losers: Set<Long>, kos: Map<Long, Int>, cupFor: Long? = null) {
        for (i in roster.indices) {
            val f = roster[i]
            var n = f
            if (f.id in winners) n = n.copy(wins = n.wins + 1)
            if (f.id in losers) n = n.copy(losses = n.losses + 1)
            kos[f.id]?.let { n = n.copy(kos = n.kos + it) }
            if (f.id == cupFor) n = n.copy(cups = n.cups + 1)
            if (n != f) roster[i] = n
        }
        matchesPlayed++
        totalKos += kos.values.sum()
        prefs.edit().putInt("matches", matchesPlayed).putInt("totalkos", totalKos).apply()
        save()
    }

    fun recordWave(w: Int) {
        if (w > bestWave) {
            bestWave = w
            prefs.edit().putInt("bestwave", w).apply()
        }
    }

    fun resetStats() {
        for (i in roster.indices) roster[i] = roster[i].copy(wins = 0, losses = 0, kos = 0, cups = 0)
        bestWave = 0; totalKos = 0; matchesPlayed = 0
        prefs.edit().putInt("bestwave", 0).putInt("totalkos", 0).putInt("matches", 0).apply()
        save()
    }

    fun changeSound(v: Boolean) { sound = v; prefs.edit().putBoolean("sound", v).apply() }
    fun changeHaptics(v: Boolean) { haptics = v; prefs.edit().putBoolean("haptics", v).apply() }
    fun changeDamageNumbers(v: Boolean) { damageNumbers = v; prefs.edit().putBoolean("dmgnums", v).apply() }
    fun changeScreenShake(v: Boolean) { screenShake = v; prefs.edit().putBoolean("shake", v).apply() }
    fun changeDynamicColor(v: Boolean) { dynamicColor = v; prefs.edit().putBoolean("dyncolor", v).apply() }
    fun changeThemeMode(v: ThemeMode) { themeMode = v; prefs.edit().putInt("theme", v.ordinal).apply() }
    fun changeGameSpeed(v: Float) { gameSpeed = v; prefs.edit().putFloat("speed", v).apply() }

    // one fighter per line: id \t name \t code \t wins \t losses \t kos \t cups
    private fun save() {
        val s = roster.joinToString("\n") {
            listOf(it.id, it.name.replace('\t', ' ').replace('\n', ' '), it.loadout.encode(), it.wins, it.losses, it.kos, it.cups).joinToString("\t")
        }
        prefs.edit().putString("roster", s).apply()
    }

    private fun load() {
        val s = prefs.getString("roster", null) ?: return
        for (line in s.split("\n")) {
            val p = line.split("\t")
            if (p.size < 3) continue
            val l = Loadout.decode(p[2]) ?: continue
            roster += RosterFighter(
                p[0].toLongOrNull() ?: continue, p[1], l,
                p.getOrNull(3)?.toIntOrNull() ?: 0, p.getOrNull(4)?.toIntOrNull() ?: 0,
                p.getOrNull(5)?.toIntOrNull() ?: 0, p.getOrNull(6)?.toIntOrNull() ?: 0,
            )
        }
    }
}
