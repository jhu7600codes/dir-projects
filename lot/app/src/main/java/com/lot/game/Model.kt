package com.lot.game

import androidx.compose.ui.graphics.Color
import kotlin.math.min
import kotlin.random.Random

enum class Mood(val label: String) {
    HAPPY("happy"), MEH("meh"), GRUMPY("grumpy"), ANGRY("angry");

    fun worse() = entries[min(ordinal + 1, entries.size - 1)]
}

// ---------- scratch tickets ----------

enum class Sym { SOCK, CHERRY, BELL, STAR, CLOVER, SEVEN, DIAMOND, SKULL }

/** MATCH3: find 3 equal symbols. NUMBERS: your numbers vs winning numbers. CURSED: match3, but 3 skulls costs you. */
enum class Kind { MATCH3, NUMBERS, CURSED }

data class Tier(
    val id: Int,
    val name: String,
    val price: Int,
    val kind: Kind,
    val winChance: Float,
    val prizes: List<Pair<Sym, Int>>,
    val weights: List<Int>,
    val paper: Color,
    val accent: Color,
    val blurb: String,
    val numPrizes: List<Int> = emptyList(),
    val flag: String? = null,
) {
    val topPrize get() = (prizes.map { it.second } + numPrizes).max()
    fun prizeOf(s: Sym) = prizes.first { it.first == s }.second
}

val Tiers = listOf(
    Tier(
        0, "pocket change", 2, Kind.MATCH3, 0.38f,
        listOf(Sym.SOCK to 1, Sym.CHERRY to 3, Sym.BELL to 6, Sym.STAR to 12, Sym.SEVEN to 40),
        listOf(40, 30, 15, 10, 5), Color(0xFFE6E0D0), Color(0xFF9E9E9E), "found in the couch",
    ),
    Tier(
        1, "paper scratcher", 10, Kind.MATCH3, 0.36f,
        listOf(Sym.SOCK to 5, Sym.CHERRY to 15, Sym.BELL to 30, Sym.STAR to 60, Sym.SEVEN to 150),
        listOf(40, 30, 15, 10, 5), Color(0xFFF3E9D2), Color(0xFF8D6E63), "the classic",
    ),
    Tier(
        2, "cursed ticket", 13, Kind.CURSED, 0.42f,
        listOf(Sym.SKULL to -26, Sym.CHERRY to 20, Sym.BELL to 40, Sym.STAR to 66, Sym.SEVEN to 666),
        listOf(38, 28, 18, 12, 4), Color(0xFF3B2F4A), Color(0xFF8A1C3A), "3 skulls = it takes 26 more",
        flag = "cursedbuy",
    ),
    Tier(
        3, "number cruncher", 20, Kind.NUMBERS, 0.34f, emptyList(),
        listOf(35, 28, 18, 11, 6, 2), Color(0xFFDDF7E9), Color(0xFF2E9E5B), "match your numbers to the winners",
        numPrizes = listOf(10, 25, 40, 100, 500, 2000),
    ),
    Tier(
        4, "shiny scratcher", 50, Kind.MATCH3, 0.32f,
        listOf(Sym.CHERRY to 40, Sym.BELL to 80, Sym.STAR to 200, Sym.CLOVER to 500, Sym.SEVEN to 1500),
        listOf(40, 28, 18, 10, 4), Color(0xFFCFE3F5), Color(0xFF3F7CC4), "now with extra shine",
    ),
    Tier(
        5, "big numbers", 100, Kind.NUMBERS, 0.3f, emptyList(),
        listOf(35, 28, 18, 11, 6, 2), Color(0xFFFFE1EC), Color(0xFFD63D73), "bigger numbers, bigger regret",
        numPrizes = listOf(50, 120, 200, 500, 2500, 25000),
    ),
    Tier(
        6, "golden scratcher", 200, Kind.MATCH3, 0.28f,
        listOf(Sym.BELL to 150, Sym.STAR to 400, Sym.CLOVER to 1000, Sym.DIAMOND to 3000, Sym.SEVEN to 20000),
        listOf(40, 28, 18, 10, 4), Color(0xFFFFE08A), Color(0xFFC48A00), "for people with too much money",
        flag = "gold",
    ),
    Tier(
        7, "diamond scratcher", 1000, Kind.MATCH3, 0.26f,
        listOf(Sym.BELL to 800, Sym.STAR to 1500, Sym.CLOVER to 4000, Sym.DIAMOND to 12000, Sym.SEVEN to 100000),
        listOf(40, 28, 18, 10, 4), Color(0xFFD8FBFF), Color(0xFF00A3C4), "whale territory",
        flag = "diamond",
    ),
)

data class ScratchTicket(
    val uid: Long,
    val tier: Tier,
    val cells: List<Sym>,
    val winSym: Sym?,
    val prize: Int,
    val winNums: List<Int> = emptyList(),
    val myNums: List<Int> = emptyList(),
    val myPrizes: List<Int> = emptyList(),
)

private fun <T> weighted(items: List<T>, weights: List<Int>, rng: Random): T {
    var r = rng.nextInt(weights.sum())
    for (i in items.indices) {
        r -= weights[i]
        if (r < 0) return items[i]
    }
    return items.last()
}

fun genScratch(tier: Tier, rng: Random = Random): ScratchTicket =
    if (tier.kind == Kind.NUMBERS) genNumbers(tier, rng) else genMatch3(tier, rng)

private fun genMatch3(tier: Tier, rng: Random): ScratchTicket {
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

private fun genNumbers(tier: Tier, rng: Random): ScratchTicket {
    val pool = (1..30).shuffled(rng)
    val winNums = pool.take(2)
    val mine = pool.drop(2).take(8).toMutableList()
    val prizes = List(8) { weighted(tier.numPrizes, tier.weights, rng) }
    if (rng.nextFloat() < tier.winChance) {
        val spots = (0 until 8).shuffled(rng)
        mine[spots[0]] = winNums[0]
        if (rng.nextFloat() < 0.15f) mine[spots[1]] = winNums[1]
    }
    val prize = mine.indices.filter { mine[it] in winNums }.sumOf { prizes[it] }
    return ScratchTicket(System.nanoTime(), tier, emptyList(), null, prize, winNums, mine, prizes)
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

/** a winning ticket you still have to cash at the stand */
data class Unclaimed(val id: Long, val name: String, val prize: Int, val time: Long)

data class Currency(val code: String, val name: String, val perCoin: Double, val decimals: Int)

/** completely made up "exchange rates". you can't withdraw anything, ever. */
val Currencies = listOf(
    Currency("usd", "us dollars", 0.01, 2),
    Currency("eur", "euros", 0.0092, 2),
    Currency("rub", "rubles", 0.9, 2),
    Currency("gbp", "pounds", 0.0079, 2),
    Currency("jpy", "yen", 1.5, 0),
    Currency("uah", "hryvnias", 0.41, 2),
    Currency("kzt", "tenge", 5.0, 0),
    Currency("btc", "bitcoin", 0.0000001, 8),
    Currency("shawarma", "shawarmas", 0.0035, 3),
    Currency("potato", "potatoes", 0.05, 1),
)

fun currencyOf(code: String) = Currencies.firstOrNull { it.code == code } ?: Currencies[0]

// ---------- items (permanent upgrades) ----------

data class Item(val id: String, val name: String, val price: Int, val desc: String, val mode: String)

val Items = listOf(
    Item("sock", "lucky sock", 300, "+4% win chance on every scratch ticket. unwashed for luck.", "player"),
    Item("sleeve", "ticket sleeve", 400, "blocks the first bit of damage on every claim walk", "claim"),
    Item("shoes", "running shoes", 250, "walk faster and jump higher on the way to the stand", "claim"),
    Item("whistle", "loud whistle", 350, "ticket thieves pull 30% weaker", "claim"),
    Item("lawyer", "lawyer's business card", 800, "the cashier starts the fight with 2 less patience", "claim"),
    Item("coffee", "coffee thermos", 300, "host mode customers wait 30% longer before leaving", "host"),
    Item("lens", "pro binoculars", 400, "bigger lens and stronger zoom in stealer mode", "stealer"),
    Item("dauber", "auto dauber", 250, "bingo numbers on your card daub themselves after 2 seconds", "bingo"),
    Item("magnet", "suspicious magnet", 450, "slot reels stop exactly when you tap, no drift", "slots"),
    Item("glasses", "reading glasses", 300, "inspector gets 40% more time per ticket", "inspector"),
    Item("birdseed", "bag of birdseed", 200, "your derby pigeon gets a small speed boost. totally legal.", "derby"),
)

// ---------- daily quests ----------

data class QuestDef(val id: String, val desc: String, val target: Int, val reward: Int, val stat: (GameState) -> Int)

val QuestPool = listOf(
    QuestDef("scratch5", "scratch 5 tickets", 5, 60) { it.scratched },
    QuestDef("tv2", "watch 2 tv draws", 2, 60) { it.tvPlayed },
    QuestDef("yell3", "yell at 3 customers", 3, 50) { it.yells },
    QuestDef("serve6", "serve 6 customers as host", 6, 80) { it.served },
    QuestDef("steal1", "steal a winning ticket", 1, 100) { it.steals },
    QuestDef("bingo1", "win a round of bingo", 1, 120) { it.bingoWins },
    QuestDef("bags8", "search 8 trash bags", 8, 70) { it.bagsSearched },
    QuestDef("claim1", "claim a prize the hard way", 1, 120) { it.claims },
    QuestDef("derby3", "bet on 3 pigeon races", 3, 60) { it.derbyBets },
    QuestDef("slots10", "spin the slots 10 times", 10, 60) { it.slotSpins },
    QuestDef("inspect15", "inspect 15 tickets", 15, 70) { it.inspected },
    QuestDef("win3", "win 3 tickets of any kind", 3, 90) { it.wins },
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
    // claiming
    val unclaimed: List<Unclaimed> = emptyList(),
    val claims: Int = 0,
    // bingo
    val bingoPlayed: Int = 0,
    val bingoWins: Int = 0,
    // dumpster
    val bagsSearched: Int = 0,
    val trashWinners: Int = 0,
    // derby
    val derbyBets: Int = 0,
    val derbyWins: Int = 0,
    // slots + inspector
    val slotSpins: Int = 0,
    val slotSevens: Int = 0,
    val inspected: Int = 0,
    val inspectBest: Int = 0,
    // items + quests
    val items: Set<String> = emptySet(),
    val questDay: Long = -1,
    val questIds: List<String> = emptyList(),
    val questBase: Map<String, Int> = emptyMap(),
    val questsDone: Set<String> = emptySet(),
    val questsCompleted: Int = 0,
    // misc
    val currency: String = "usd",
    val currenciesSeen: Set<String> = emptySet(),
    val lastWheelDay: Long = -1,
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
    BadgeDef("diamond", "whale", "buy a diamond scratcher", "player") { "diamond" in it.flags },
    BadgeDef("cursed", "hex'd", "hit 3 skulls on a cursed ticket", "player") { "cursed" in it.flags },
    BadgeDef("wheel", "spin doctor", "spin the daily wheel", "player") { "wheel" in it.flags },
    BadgeDef("signed", "john hancock", "sign a winning ticket", "claim") { "signed" in it.flags },
    BadgeDef("claim1", "cash money", "claim a prize the hard way", "claim") { it.claims >= 1 },
    BadgeDef("claim10", "frequent fighter", "claim 10 prizes the hard way", "claim") { it.claims >= 10 },
    BadgeDef("clean_walk", "untouched", "reach the stand with zero ticket damage", "claim") { "cleanwalk" in it.flags },
    BadgeDef("objection", "objection!", "beat the cashier without taking a hit", "claim") { "flawless" in it.flags },
    BadgeDef("denied", "denied", "lose a fight to the cashier", "claim") { "denied" in it.flags },
    BadgeDef("not_today", "not today", "fight off a ticket thief", "claim") { "thiefwin" in it.flags },
    BadgeDef("yoink", "yoink", "lose a ticket to a thief", "claim") { "thieflose" in it.flags },
    BadgeDef("lazy", "convenience fee", "use the lazy cash-out", "claim") { "lazy" in it.flags },
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

    BadgeDef("bingo1", "bingo!", "win a round of bingo", "bingo") { it.bingoWins >= 1 },
    BadgeDef("bingo5", "granny slayer", "win 5 rounds of bingo", "bingo") { it.bingoWins >= 5 },
    BadgeDef("falsebingo", "false alarm", "yell bingo without having bingo", "bingo") { "falsebingo" in it.flags },
    BadgeDef("trash1", "trash treasure", "find a winning ticket in the trash", "dumpster") { it.trashWinners >= 1 },
    BadgeDef("trash50", "raccoon energy", "search 50 trash bags", "dumpster") { it.bagsSearched >= 50 },
    BadgeDef("raccoon", "trash panda", "fight off a raccoon", "dumpster") { "raccoon" in it.flags },
    BadgeDef("derby1", "bird brain", "win a pigeon race bet", "derby") { it.derbyWins >= 1 },
    BadgeDef("longshot", "long shot", "win on a 10x pigeon", "derby") { "longshot" in it.flags },
    BadgeDef("derby10", "pigeon whisperer", "win 10 pigeon race bets", "derby") { it.derbyWins >= 10 },
    BadgeDef("slots25", "one-armed bandit", "spin the slots 25 times", "slots") { it.slotSpins >= 25 },
    BadgeDef("triple7", "triple seven", "hit 3 sevens on the slots", "slots") { it.slotSevens >= 1 },
    BadgeDef("skullslot", "bad omen", "hit 3 skulls on the slots", "slots") { "skullslot" in it.flags },
    BadgeDef("inspect50", "rubber stamp", "inspect 50 tickets", "inspector") { it.inspected >= 50 },
    BadgeDef("streak20", "nothing gets past me", "get a 20 ticket streak as inspector", "inspector") { it.inspectBest >= 20 },
    BadgeDef("item1", "retail therapy", "buy your first item", "all") { it.items.isNotEmpty() },
    BadgeDef("item5", "shopaholic", "own 5 items", "all") { it.items.size >= 5 },
    BadgeDef("itemall", "fully loaded", "own every item", "all") { it.items.size >= Items.size },
    BadgeDef("quest1", "to-do list", "finish a daily quest", "all") { it.questsCompleted >= 1 },
    BadgeDef("quest15", "productive citizen", "finish 15 daily quests", "all") { it.questsCompleted >= 15 },
    BadgeDef("economist", "economist", "check what coins are worth in 3 currencies", "all") { it.currenciesSeen.size >= 3 },
    BadgeDef("all", "jack of all trades", "play all 8 modes", "all") { it.modesPlayed.size >= 8 },
)
