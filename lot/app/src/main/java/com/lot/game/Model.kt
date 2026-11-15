package com.lot.game

import androidx.compose.ui.graphics.Color
import kotlin.math.min
import kotlin.random.Random

enum class Mood(val label: String) {
    HAPPY("happy"), MEH("meh"), GRUMPY("grumpy"), ANGRY("angry");

    fun worse() = entries[min(ordinal + 1, entries.size - 1)]
}

// ---------- scratch tickets ----------

enum class Sym { SOCK, CHERRY, BELL, STAR, CLOVER, SEVEN, DIAMOND }

data class Tier(
    val id: Int,
    val name: String,
    val price: Int,
    val winChance: Float,
    val prizes: List<Pair<Sym, Int>>,
    val weights: List<Int>,
    val paper: Color,
    val accent: Color,
) {
    val topPrize get() = prizes.maxOf { it.second }
    fun prizeOf(s: Sym) = prizes.first { it.first == s }.second
}

val Tiers = listOf(
    Tier(
        0, "paper scratcher", 10, 0.36f,
        listOf(Sym.SOCK to 5, Sym.CHERRY to 15, Sym.BELL to 30, Sym.STAR to 60, Sym.SEVEN to 150),
        listOf(40, 30, 15, 10, 5), Color(0xFFF3E9D2), Color(0xFF8D6E63),
    ),
    Tier(
        1, "shiny scratcher", 50, 0.32f,
        listOf(Sym.CHERRY to 40, Sym.BELL to 80, Sym.STAR to 200, Sym.CLOVER to 500, Sym.SEVEN to 1500),
        listOf(40, 28, 18, 10, 4), Color(0xFFCFE3F5), Color(0xFF3F7CC4),
    ),
    Tier(
        2, "golden scratcher", 200, 0.28f,
        listOf(Sym.BELL to 150, Sym.STAR to 400, Sym.CLOVER to 1000, Sym.DIAMOND to 3000, Sym.SEVEN to 20000),
        listOf(40, 28, 18, 10, 4), Color(0xFFFFE08A), Color(0xFFC48A00),
    ),
)

data class ScratchTicket(
    val uid: Long,
    val tier: Tier,
    val cells: List<Sym>,
    val winSym: Sym?,
    val prize: Int,
)

private fun <T> weighted(items: List<T>, weights: List<Int>, rng: Random): T {
    var r = rng.nextInt(weights.sum())
    for (i in items.indices) {
        r -= weights[i]
        if (r < 0) return items[i]
    }
    return items.last()
}

fun genScratch(tier: Tier, rng: Random = Random): ScratchTicket {
    val syms = tier.prizes.map { it.first }
    val win = rng.nextFloat() < tier.winChance
    val cells = mutableListOf<Sym>()
    val counts = mutableMapOf<Sym, Int>()
    var winSym: Sym? = null
    if (win) {
        val w = weighted(syms, tier.weights, rng)
        winSym = w
        repeat(3) { cells.add(w) }
        counts[w] = 3
    }
    while (cells.size < 9) {
        val s = syms.random(rng)
        if (s != winSym && (counts[s] ?: 0) < 2) {
            cells.add(s); counts[s] = (counts[s] ?: 0) + 1
        }
    }
    cells.shuffle(rng)
    return ScratchTicket(
        System.nanoTime(), tier, cells, winSym,
        winSym?.let { tier.prizeOf(it) } ?: 0,
    )
}

// ---------- tv tickets ----------

const val TV_PRICE = 25
const val TV_POOL = 30
const val TV_PICKS = 5

fun tvPrize(matches: Int) = when (matches) {
    2 -> 40
    3 -> 400
    4 -> 5000
    5 -> 100000
    else -> 0
}

data class TvTicket(val uid: Long, val picks: List<Int>, val draws: List<Int>) {
    val matches get() = draws.count { it in picks }
    val prize get() = tvPrize(matches)
}

fun genTv(picks: List<Int>, rng: Random = Random): TvTicket {
    val draws = if (rng.nextFloat() < 0.06f) {
        // a little rigged in your favor, so the show stays exciting
        val others = (1..TV_POOL).filter { it !in picks }.shuffled(rng)
        (picks.shuffled(rng).take(3) + others.take(2)).shuffled(rng)
    } else {
        (1..TV_POOL).shuffled(rng).take(TV_PICKS)
    }
    return TvTicket(System.nanoTime(), picks.sorted(), draws)
}

// ---------- persisted state ----------

data class TicketRecord(
    val type: String,
    val cost: Int,
    val prize: Int,
    val time: Long,
    val detail: String,
)

data class GameState(
    val coins: Int = 500,
    val history: List<TicketRecord> = emptyList(),
    val ticketsBought: Int = 0,
    val scratched: Int = 0,
    val tvPlayed: Int = 0,
    val totalSpent: Int = 0,
    val totalWon: Int = 0,
    val biggestWin: Int = 0,
    val wins: Int = 0,
    // host
    val hostDays: Int = 0,
    val hostTotalProfit: Int = 0,
    val reputation: Int = 50,
    val yells: Int = 0,
    val served: Int = 0,
    val bestDayProfit: Int = 0,
    // stealer
    val steals: Int = 0,
    val perfectSteals: Int = 0,
    val timesCaught: Int = 0,
    val bestRound: Int = 0,
    val stealLoot: Int = 0,
    // meta
    val flags: Set<String> = emptySet(),
    val badges: Map<String, Long> = emptyMap(),
    val tutorialsSeen: Set<String> = emptySet(),
    val modesPlayed: Set<String> = emptySet(),
    val sound: Boolean = true,
    val haptics: Boolean = true,
)

// ---------- badges ----------

data class BadgeDef(
    val id: String,
    val name: String,
    val desc: String,
    val mode: String,
    val check: (GameState) -> Boolean,
)

val Badges = listOf(
    BadgeDef("first_ticket", "fresh meat", "buy your first ticket", "player") { it.ticketsBought >= 1 },
    BadgeDef("first_win", "beginner's luck", "win any prize", "player") { it.wins >= 1 },
    BadgeDef("scratch10", "itchy fingers", "scratch 10 tickets", "player") { it.scratched >= 10 },
    BadgeDef("scratch50", "scratch addict", "scratch 50 tickets", "player") { it.scratched >= 50 },
    BadgeDef("gold", "high roller", "buy a golden scratcher", "player") { "gold" in it.flags },
    BadgeDef("jackpot", "big money", "win 1,000+ coins on one ticket", "player") { it.biggestWin >= 1000 },
    BadgeDef("tv1", "on air", "watch your first tv draw", "player") { it.tvPlayed >= 1 },
    BadgeDef("tv3", "three's company", "match 3+ numbers in a tv draw", "player") { "tv3" in it.flags },
    BadgeDef("broke", "flat broke", "run out of coins and beg mom", "player") { "begged" in it.flags },
    BadgeDef("rich", "fake millionaire", "hold 10,000 coins at once", "player") { it.coins >= 10000 },

    BadgeDef("day1", "open for business", "finish your first day as host", "host") { it.hostDays >= 1 },
    BadgeDef("day5", "stand veteran", "finish 5 days as host", "host") { it.hostDays >= 5 },
    BadgeDef("yell5", "loudmouth", "yell at customers 5 times", "host") { it.yells >= 5 },
    BadgeDef("yell50", "menace to society", "yell at customers 50 times", "host") { it.yells >= 50 },
    BadgeDef("profit500", "small business", "make 500+ profit in one day", "host") { it.bestDayProfit >= 500 },
    BadgeDef("rep90", "beloved vendor", "reach 90 reputation", "host") { it.reputation >= 90 },
    BadgeDef("rep10", "health code violation", "drop to 10 reputation or less", "host") { it.reputation <= 10 },
    BadgeDef("scam", "fine print", "pretend a winner lost and get away with it", "host") { "scam" in it.flags },
    BadgeDef("served50", "people person", "serve 50 customers", "host") { it.served >= 50 },

    BadgeDef("steal1", "sticky fingers", "steal your first winning ticket", "stealer") { it.steals >= 1 },
    BadgeDef("perfect", "ghost", "pull off a perfect steal", "stealer") { it.perfectSteals >= 1 },
    BadgeDef("caught", "busted", "get caught by a guard", "stealer") { it.timesCaught >= 1 },
    BadgeDef("round5", "career criminal", "reach round 5 as a stealer", "stealer") { it.bestRound >= 5 },
    BadgeDef("dud", "it was a dud", "steal a losing ticket", "stealer") { "dud" in it.flags },
    BadgeDef("loot1000", "heist of the century", "loot 1,000 coins in total", "stealer") { it.stealLoot >= 1000 },

    BadgeDef("all", "triple threat", "play all 3 modes", "all") { it.modesPlayed.size >= 3 },
)
