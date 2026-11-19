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
    put("unclaimed", JSONArray().apply {
        unclaimed.forEach { put(JSONObject().put("id", it.id).put("n", it.name).put("p", it.prize).put("ts", it.time)) }
    })
    put("claims", claims); put("bingoPlayed", bingoPlayed); put("bingoWins", bingoWins)
    put("bagsSearched", bagsSearched); put("trashWinners", trashWinners)
    put("currency", currency); put("currenciesSeen", JSONArray(currenciesSeen.toList())); put("lastWheelDay", lastWheelDay)
    put("derbyBets", derbyBets); put("derbyWins", derbyWins)
    put("rainBest", rainBest); put("rainPlays", rainPlays); put("hiloBestStreak", hiloBestStreak); put("hiloBestCashout", hiloBestCashout)
    put("campaignLevel", campaignLevel); put("campaignBase", campaignBase)
    put("clawPlays", clawPlays); put("clawWins", clawWins); put("flightPlays", flightPlays); put("flightBest", flightBest)
    put("towerPlays", towerPlays); put("towerBest", towerBest)
    put("customTickets", JSONArray().apply { customTickets.forEach { put(it.toJson()) } })
    put("customModes", JSONArray().apply { customModes.forEach { put(it.toJson()) } })
    put("nickname", nickname); put("lockerSecret", lockerSecret); put("lockerUploads", lockerUploads)
    put("lockerSaved", JSONArray(lockerSaved.toList()))
    put("customPlays", customPlays); put("ballShakes", ballShakes); put("ballWins", ballWins)
    put("wardrobe", JSONArray(wardrobe.toList())); put("outfitHat", outfitHat); put("outfitShirt", outfitShirt); put("outfitGlasses", outfitGlasses)
    put("slotSpins", slotSpins); put("slotSevens", slotSevens); put("inspected", inspected); put("inspectBest", inspectBest)
    put("items", JSONArray(items.toList()))
    put("questDay", questDay); put("questIds", JSONArray(questIds))
    put("questBase", JSONObject().apply { questBase.forEach { (k, v) -> put(k, v) } })
    put("questsDone", JSONArray(questsDone.toList())); put("questsCompleted", questsCompleted)
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
    val unc = o.optJSONArray("unclaimed")
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
        unclaimed = if (unc == null) emptyList() else (0 until unc.length()).map {
            val u = unc.getJSONObject(it)
            Unclaimed(u.optLong("id"), u.optString("n"), u.optInt("p"), u.optLong("ts"))
        },
        claims = o.optInt("claims"), bingoPlayed = o.optInt("bingoPlayed"), bingoWins = o.optInt("bingoWins"),
        bagsSearched = o.optInt("bagsSearched"), trashWinners = o.optInt("trashWinners"),
        currency = o.optString("currency", "usd"), currenciesSeen = o.optJSONArray("currenciesSeen").strings(),
        lastWheelDay = o.optLong("lastWheelDay", -1),
        derbyBets = o.optInt("derbyBets"), derbyWins = o.optInt("derbyWins"),
        rainBest = o.optInt("rainBest"), rainPlays = o.optInt("rainPlays"),
        hiloBestStreak = o.optInt("hiloBestStreak"), hiloBestCashout = o.optInt("hiloBestCashout"),
        campaignLevel = o.optInt("campaignLevel"), campaignBase = o.optInt("campaignBase", -1),
        clawPlays = o.optInt("clawPlays"), clawWins = o.optInt("clawWins"),
        flightPlays = o.optInt("flightPlays"), flightBest = o.optInt("flightBest"),
        towerPlays = o.optInt("towerPlays"), towerBest = o.optInt("towerBest"),
        wardrobe = o.optJSONArray("wardrobe")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: d.wardrobe,
        outfitHat = o.optString("outfitHat", d.outfitHat), outfitShirt = o.optString("outfitShirt", d.outfitShirt),
        outfitGlasses = o.optBoolean("outfitGlasses"),
        customTickets = o.optJSONArray("customTickets")?.let { a -> (0 until a.length()).mapNotNull { ticketFromJson(a.getJSONObject(it)) } } ?: emptyList(),
        customModes = o.optJSONArray("customModes")?.let { a -> (0 until a.length()).map { modeFromJson(a.getJSONObject(it)) } } ?: emptyList(),
        nickname = o.optString("nickname"), lockerSecret = o.optString("lockerSecret"), lockerUploads = o.optInt("lockerUploads"),
        lockerSaved = o.optJSONArray("lockerSaved").strings(),
        customPlays = o.optInt("customPlays"), ballShakes = o.optInt("ballShakes"), ballWins = o.optInt("ballWins"),
        slotSpins = o.optInt("slotSpins"), slotSevens = o.optInt("slotSevens"),
        inspected = o.optInt("inspected"), inspectBest = o.optInt("inspectBest"),
        items = o.optJSONArray("items").strings(),
        questDay = o.optLong("questDay", -1),
        questIds = o.optJSONArray("questIds")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        questBase = o.optJSONObject("questBase")?.let { b -> b.keys().asSequence().associateWith { b.getInt(it) } } ?: emptyMap(),
        questsDone = o.optJSONArray("questsDone").strings(),
        questsCompleted = o.optInt("questsCompleted"),
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
                flags = if (tier.flag != null) it.flags + tier.flag else it.flags,
            )
        }
        val bonus = if ("sock" in _state.value.items) 0.04f else 0f
        currentScratch = genScratch(tier.copy(winChance = tier.winChance + bonus))
        return true
    }

    fun settleScratch(t: ScratchTicket) {
        if (!settled.add(t.uid)) return
        val now = System.currentTimeMillis()
        update {
            it.copy(
                coins = if (t.prize < 0) (it.coins + t.prize).coerceAtLeast(0) else it.coins,
                scratched = it.scratched + 1,
                wins = it.wins + if (t.prize > 0) 1 else 0,
                flags = if (t.prize < 0) it.flags + "cursed" else it.flags,
                unclaimed = if (t.prize > 0) it.unclaimed + Unclaimed(t.uid, t.tier.name, t.prize, now) else it.unclaimed,
                history = record(
                    it, TicketRecord(
                        t.tier.name, t.tier.price, t.prize, now,
                        when {
                            t.prize < 0 -> "cursed! 3 skulls"
                            t.tier.kind == Kind.NUMBERS -> if (t.prize > 0) "numbers matched" else "no match"
                            t.winSym != null -> "3x ${t.winSym.name.lowercase()}"
                            else -> "no match"
                        },
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
        val now = System.currentTimeMillis()
        update {
            it.copy(
                tvPlayed = it.tvPlayed + 1,
                wins = it.wins + if (t.prize > 0) 1 else 0,
                unclaimed = if (t.prize > 0) it.unclaimed + Unclaimed(t.uid, "tv ticket", t.prize, now) else it.unclaimed,
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

    // ---- claiming prizes ----
    var currentClaim by mutableStateOf<Unclaimed?>(null)
        private set

    fun startClaim(id: Long): Boolean {
        val u = _state.value.unclaimed.firstOrNull { it.id == id } ?: return false
        currentClaim = u
        return true
    }

    /** pays out a claimed ticket and removes it from the unclaimed list */
    fun payClaim(id: Long, amount: Int, hardWay: Boolean, extraFlags: Set<String> = emptySet()) {
        update { s ->
            if (s.unclaimed.none { it.id == id }) return@update s
            s.copy(
                unclaimed = s.unclaimed.filter { it.id != id },
                coins = s.coins + amount,
                totalWon = s.totalWon + amount,
                biggestWin = maxOf(s.biggestWin, amount),
                claims = s.claims + if (hardWay) 1 else 0,
                flags = s.flags + extraFlags,
            )
        }
    }

    fun loseClaim(id: Long, extraFlags: Set<String> = emptySet()) =
        update { s -> s.copy(unclaimed = s.unclaimed.filter { it.id != id }, flags = s.flags + extraFlags) }

    fun lazyCash(id: Long) {
        val u = _state.value.unclaimed.firstOrNull { it.id == id } ?: return
        payClaim(id, u.prize * 7 / 10, false, setOf("lazy"))
    }

    fun addUnclaimed(name: String, prize: Int) = update {
        it.copy(unclaimed = it.unclaimed + Unclaimed(System.nanoTime(), name, prize, System.currentTimeMillis()))
    }

    fun flag(f: String) = update { if (f in it.flags) it else it.copy(flags = it.flags + f) }

    // ---- misc ----
    fun today() = System.currentTimeMillis() / 86_400_000L

    fun spinWheel(prize: Int) = update {
        it.copy(coins = it.coins + prize, lastWheelDay = today(), flags = it.flags + "wheel")
    }

    fun setCurrency(code: String) = update { it.copy(currency = code, currenciesSeen = it.currenciesSeen + code) }

    // ---- items ----
    fun buyItem(item: Item): Boolean {
        val s = _state.value
        if (item.id in s.items || s.coins < item.price) return false
        update { it.copy(coins = it.coins - item.price, items = it.items + item.id) }
        return true
    }

    // ---- daily quests ----
    fun refreshQuests() {
        val day = today()
        val s = _state.value
        if (s.questDay == day && s.questIds.isNotEmpty()) return
        val picks = QuestPool.shuffled(kotlin.random.Random(day)).take(3)
        update {
            it.copy(
                questDay = day,
                questIds = picks.map { q -> q.id },
                questBase = picks.associate { q -> q.id to q.stat(it) },
                questsDone = emptySet(),
            )
        }
    }

    fun questProgress(s: GameState, q: QuestDef) = (q.stat(s) - (s.questBase[q.id] ?: q.stat(s))).coerceIn(0, q.target)

    fun collectQuest(q: QuestDef) {
        val s = _state.value
        if (q.id in s.questsDone || questProgress(s, q) < q.target) return
        update {
            it.copy(
                coins = it.coins + q.reward,
                questsDone = it.questsDone + q.id,
                questsCompleted = it.questsCompleted + 1,
            )
        }
    }

    // ---- campaign ----
    fun campaignStart() {
        val s = _state.value
        val lv = Campaign.getOrNull(s.campaignLevel) ?: return
        if (s.campaignBase < 0) update { it.copy(campaignBase = lv.stat(it)) }
    }

    fun campaignProgress(s: GameState): Int {
        val lv = Campaign.getOrNull(s.campaignLevel) ?: return 0
        val v = lv.stat(s)
        return (if (lv.absolute) v else v - (if (s.campaignBase < 0) v else s.campaignBase)).coerceIn(0, lv.target)
    }

    fun campaignComplete() {
        val s = _state.value
        val lv = Campaign.getOrNull(s.campaignLevel) ?: return
        if (campaignProgress(s) < lv.target) return
        update {
            val next = Campaign.getOrNull(it.campaignLevel + 1)
            it.copy(
                coins = it.coins + lv.reward,
                campaignLevel = it.campaignLevel + 1,
                campaignBase = next?.stat?.invoke(it) ?: -1,
            )
        }
    }

    // ---- wardrobe ----
    fun buyCosmetic(c: Cosmetic): Boolean {
        val s = _state.value
        if (c.id in s.wardrobe || c.price <= 0 || s.coins < c.price) return false
        update { it.copy(coins = it.coins - c.price, wardrobe = it.wardrobe + c.id) }
        return true
    }

    fun giveCosmetic(id: String) = update { it.copy(wardrobe = it.wardrobe + id) }

    fun wear(c: Cosmetic) = update {
        if (c.id !in it.wardrobe) it else when (c.kind) {
            "hat" -> it.copy(outfitHat = c.id)
            "shirt" -> it.copy(outfitShirt = c.id)
            else -> it.copy(outfitGlasses = !it.outfitGlasses)
        }
    }

    // ---- creator ----
    fun saveTicket(t: CustomTicket) = update { s -> s.copy(customTickets = s.customTickets.filter { it.id != t.id } + t) }
    fun deleteTicket(id: Long) = update { s -> s.copy(customTickets = s.customTickets.filter { it.id != id }) }
    fun saveMode(m: CustomMode) = update { s -> s.copy(customModes = s.customModes.filter { it.id != m.id } + m) }
    fun deleteMode(id: Long) = update { s -> s.copy(customModes = s.customModes.filter { it.id != id }) }

    var currentMode by mutableStateOf<CustomMode?>(null)

    // ---- lottery locker ----
    fun ensureSecret(): String {
        val s = _state.value.lockerSecret
        if (s.length >= 16) return s
        val fresh = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().replace("-", "")
        update { it.copy(lockerSecret = fresh) }
        return fresh
    }

    fun setNickname(n: String) = update { it.copy(nickname = n.trim().take(24)) }

    /** saves a locker item into your local stuff. returns a message for the player */
    fun saveFromLocker(item: LockerItem): String {
        val s = _state.value
        val msg = when (item.kind) {
            "mode" -> {
                val m = modeFromJson(item.data).copy(id = System.currentTimeMillis(), author = item.author)
                saveMode(m); "\"${m.name}\" added to your modes"
            }
            "ticket" -> {
                val t = ticketFromJson(item.data)?.copy(id = System.currentTimeMillis()) ?: return "this ticket is broken"
                saveTicket(t); "\"${t.name}\" is in your shop now"
            }
            else -> {
                val hat = item.data.optString("hat"); val shirt = item.data.optString("shirt")
                val missing = listOf(hat, shirt).filter { it.isNotBlank() && it !in s.wardrobe }.mapNotNull { cosmetic(it)?.name }
                if (missing.isNotEmpty()) return "you don't own: ${missing.joinToString()}"
                update {
                    it.copy(
                        outfitHat = cosmetic(hat)?.takeIf { c -> c.kind == "hat" }?.id ?: it.outfitHat,
                        outfitShirt = cosmetic(shirt)?.takeIf { c -> c.kind == "shirt" }?.id ?: it.outfitShirt,
                        outfitGlasses = item.data.optBoolean("glasses") && "glasses" in it.wardrobe,
                    )
                }
                "outfit equipped"
            }
        }
        update { it.copy(lockerSaved = it.lockerSaved + item.id) }
        return msg
    }
}
