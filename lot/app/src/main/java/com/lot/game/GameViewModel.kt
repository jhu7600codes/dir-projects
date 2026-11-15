package com.lot.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("lot")
private val KEY = stringPreferencesKey("state")

private fun GameState.toJson(): String = JSONObject().apply {
    put("coins", coins)
    put("history", JSONArray().apply {
        history.forEach {
            put(JSONObject().put("t", it.type).put("c", it.cost).put("p", it.prize).put("ts", it.time).put("d", it.detail))
        }
    })
    put("ticketsBought", ticketsBought); put("scratched", scratched); put("tvPlayed", tvPlayed)
    put("totalSpent", totalSpent); put("totalWon", totalWon); put("biggestWin", biggestWin); put("wins", wins)
    put("hostDays", hostDays); put("hostTotalProfit", hostTotalProfit); put("reputation", reputation)
    put("yells", yells); put("served", served); put("bestDayProfit", bestDayProfit)
    put("steals", steals); put("perfectSteals", perfectSteals); put("timesCaught", timesCaught)
    put("bestRound", bestRound); put("stealLoot", stealLoot)
    put("flags", JSONArray(flags.toList()))
    put("badges", JSONObject().apply { badges.forEach { (k, v) -> put(k, v) } })
    put("tutorialsSeen", JSONArray(tutorialsSeen.toList()))
    put("modesPlayed", JSONArray(modesPlayed.toList()))
    put("sound", sound); put("haptics", haptics)
}.toString()

private fun JSONArray?.strings(): Set<String> =
    if (this == null) emptySet() else (0 until length()).map { getString(it) }.toSet()

private fun parseState(s: String): GameState = try {
    val o = JSONObject(s)
    val d = GameState()
    val hist = o.optJSONArray("history")
    GameState(
        coins = o.optInt("coins", d.coins),
        history = if (hist == null) emptyList() else (0 until hist.length()).map {
            val h = hist.getJSONObject(it)
            TicketRecord(h.optString("t"), h.optInt("c"), h.optInt("p"), h.optLong("ts"), h.optString("d"))
        },
        ticketsBought = o.optInt("ticketsBought"), scratched = o.optInt("scratched"), tvPlayed = o.optInt("tvPlayed"),
        totalSpent = o.optInt("totalSpent"), totalWon = o.optInt("totalWon"), biggestWin = o.optInt("biggestWin"),
        wins = o.optInt("wins"),
        hostDays = o.optInt("hostDays"), hostTotalProfit = o.optInt("hostTotalProfit"),
        reputation = o.optInt("reputation", d.reputation), yells = o.optInt("yells"), served = o.optInt("served"),
        bestDayProfit = o.optInt("bestDayProfit"),
        steals = o.optInt("steals"), perfectSteals = o.optInt("perfectSteals"), timesCaught = o.optInt("timesCaught"),
        bestRound = o.optInt("bestRound"), stealLoot = o.optInt("stealLoot"),
        flags = o.optJSONArray("flags").strings(),
        badges = o.optJSONObject("badges")?.let { b -> b.keys().asSequence().associateWith { b.getLong(it) } } ?: emptyMap(),
        tutorialsSeen = o.optJSONArray("tutorialsSeen").strings(),
        modesPlayed = o.optJSONArray("modesPlayed").strings(),
        sound = o.optBoolean("sound", true), haptics = o.optBoolean("haptics", true),
    )
} catch (e: Exception) {
    GameState()
}

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val ds = app.dataStore
    private val _state = MutableStateFlow(GameState())
    val state = _state.asStateFlow()
    var loaded by mutableStateOf(false)
        private set

    /** badges waiting to be shown as a popup, oldest first */
    val badgeQueue = mutableStateListOf<BadgeDef>()

    var currentScratch by mutableStateOf<ScratchTicket?>(null)
        private set
    var currentTv by mutableStateOf<TvTicket?>(null)
        private set
    private val settled = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            val raw = ds.data.first()[KEY]
            _state.value = raw?.let { parseState(it) } ?: GameState()
            loaded = true
            _state.drop(1).collectLatest { s ->
                launch(Dispatchers.IO) { ds.edit { it[KEY] = s.toJson() } }.join()
            }
        }
    }

    fun update(f: (GameState) -> GameState) {
        val old = _state.value
        var s = f(old)
        val now = System.currentTimeMillis()
        val fresh = Badges.filter { it.id !in s.badges && it.check(s) }
        if (fresh.isNotEmpty()) s = s.copy(badges = s.badges + fresh.associate { it.id to now })
        _state.value = s
        badgeQueue.addAll(fresh)
    }

    fun popBadge() {
        if (badgeQueue.isNotEmpty()) badgeQueue.removeAt(0)
    }

    fun markTutorial(mode: String) = update { it.copy(tutorialsSeen = it.tutorialsSeen + mode) }
    fun markPlayed(mode: String) = update {
        if (mode in it.modesPlayed) it else it.copy(modesPlayed = it.modesPlayed + mode)
    }

    fun setSound(on: Boolean) = update { it.copy(sound = on) }
    fun setHaptics(on: Boolean) = update { it.copy(haptics = on) }
    fun resetAll() {
        val s = _state.value
        badgeQueue.clear()
        _state.value = GameState(sound = s.sound, haptics = s.haptics)
    }

    fun begMom() = update { it.copy(coins = it.coins + 100, flags = it.flags + "begged") }

    private fun record(s: GameState, r: TicketRecord) = (listOf(r) + s.history).take(100)

    // ---- scratch ----
    fun buyScratch(tier: Tier): Boolean {
        if (_state.value.coins < tier.price) return false
        currentScratch?.let { settleScratch(it) }
        update {
            it.copy(
                coins = it.coins - tier.price,
                ticketsBought = it.ticketsBought + 1,
                totalSpent = it.totalSpent + tier.price,
                flags = if (tier.id == 2) it.flags + "gold" else it.flags,
            )
        }
        currentScratch = genScratch(tier)
        return true
    }

    fun settleScratch(t: ScratchTicket) {
        if (!settled.add(t.uid)) return
        update {
            it.copy(
                coins = it.coins + t.prize,
                scratched = it.scratched + 1,
                wins = it.wins + if (t.prize > 0) 1 else 0,
                totalWon = it.totalWon + t.prize,
                biggestWin = maxOf(it.biggestWin, t.prize),
                history = record(
                    it, TicketRecord(
                        t.tier.name, t.tier.price, t.prize, System.currentTimeMillis(),
                        if (t.winSym != null) "3x ${t.winSym.name.lowercase()}" else "no match",
                    )
                ),
            )
        }
    }

    // ---- tv ----
    fun buyTv(picks: List<Int>): Boolean {
        if (_state.value.coins < TV_PRICE) return false
        currentTv?.let { settleTv(it) }
        update {
            it.copy(
                coins = it.coins - TV_PRICE,
                ticketsBought = it.ticketsBought + 1,
                totalSpent = it.totalSpent + TV_PRICE,
            )
        }
        currentTv = genTv(picks)
        return true
    }

    fun settleTv(t: TvTicket) {
        if (!settled.add(t.uid)) return
        update {
            it.copy(
                coins = it.coins + t.prize,
                tvPlayed = it.tvPlayed + 1,
                wins = it.wins + if (t.prize > 0) 1 else 0,
                totalWon = it.totalWon + t.prize,
                biggestWin = maxOf(it.biggestWin, t.prize),
                flags = if (t.matches >= 3) it.flags + "tv3" else it.flags,
                history = record(
                    it, TicketRecord(
                        "tv ticket", TV_PRICE, t.prize, System.currentTimeMillis(),
                        "${t.matches} of 5 matched",
                    )
                ),
            )
        }
    }
}
