package com.simulasim.app.crypto

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

data class Coin(
    val ticker: String,
    val name: String,
    val price: Double,
    val history: List<Double>,
    val trend: Double,
    val vol: Double,
    val rugRisk: Double,
    val launchPrice: Double,
    val rugged: Boolean = false,
    val age: Int = 0,
    val deadTicks: Int = 0,
)

enum class LaunchPhase { LIVE, RUGGED, EXPOSED }

/**
 * your own coin. liquidity is a constant-product pool (x*y=k, like uniswap),
 * so every fake buyer actually moves the price and your exit actually drains it.
 */
data class Launch(
    val name: String,
    val ticker: String,
    val poolUsd: Double,
    val poolTokens: Double,
    val devTokens: Double,
    val holderTokens: Double,
    val holders: Int,
    val hype: Double,
    val suspicion: Double,
    val locked: Boolean,
    val history: List<Double>,
    val seed: Double,
    val spent: Double,
    val cashedOut: Double,
    val ticks: Int = 0,
    val phase: LaunchPhase = LaunchPhase.LIVE,
    val result: String? = null,
) {
    val price get() = poolUsd / poolTokens
    val marketCap get() = price * TOTAL_SUPPLY
    val devValue get() = devTokens * price
}

const val TOTAL_SUPPLY = 1_000_000_000.0
const val LAUNCH_COST = 500.0
private const val START_CASH = 1000.0

class CryptoViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("crypto", 0)

    var cash by mutableDoubleStateOf(prefs.getString("cash", null)?.toDoubleOrNull() ?: START_CASH)
        private set
    var bestRug by mutableDoubleStateOf(prefs.getString("best", null)?.toDoubleOrNull() ?: 0.0)
        private set
    var timesRugged by mutableIntStateOf(prefs.getInt("rugs", 0))
        private set

    val coins = mutableStateListOf<Coin>()
    val holdings = mutableStateMapOf<String, Double>()
    val news = mutableStateListOf<String>()
    var launch by mutableStateOf<Launch?>(null)
        private set

    init {
        repeat(5) { coins.add(newCoin()) }
        news.add("market open. none of this is financial advice")
        viewModelScope.launch {
            while (true) {
                delay(700)
                tickMarket()
                tickLaunch()
            }
        }
    }

    // ---------- market ----------

    private fun newCoin(): Coin {
        val (ticker, name) = randomName()
        val p = 10.0.pow(Random.nextDouble(-7.0, -1.0))
        return Coin(
            ticker = ticker,
            name = name,
            price = p,
            history = List(40) { p },
            trend = Random.nextDouble(-0.005, 0.015),
            vol = Random.nextDouble(0.02, 0.08),
            rugRisk = Random.nextDouble(0.0008, 0.006),
            launchPrice = p,
        )
    }

    private fun Double.pow(e: Double) = Math.pow(this, e)

    private fun gauss(): Double {
        // box-muller, good enough for fake money
        val u = Random.nextDouble(1e-9, 1.0)
        val v = Random.nextDouble()
        return sqrt(-2 * kotlin.math.ln(u)) * kotlin.math.cos(2 * Math.PI * v)
    }

    private fun tickMarket() {
        for (i in coins.indices) {
            val c = coins[i]
            if (c.rugged) {
                if (c.deadTicks > 30) {
                    val bag = holdings.remove(c.ticker) ?: 0.0
                    if (bag > 0) say("${c.ticker} got delisted. your bags are now a jpeg of a bag")
                    coins[i] = newCoin().also { say("new coin just dropped: \$${it.ticker}. totally not a scam") }
                } else {
                    val p = c.price * Random.nextDouble(0.9, 1.04)
                    coins[i] = c.copy(price = p, history = (c.history + p).takeLast(60), deadTicks = c.deadTicks + 1)
                }
                continue
            }
            var trend = (c.trend * 0.96 + gauss() * 0.003).coerceIn(-0.04, 0.05)
            val r = Random.nextDouble()
            when {
                r < 0.006 -> { trend += 0.04; say(pumpLine(c.ticker)) }
                r < 0.011 -> { trend -= 0.035; say(fudLine(c.ticker)) }
            }
            var p = (c.price * exp(trend + gauss() * c.vol)).coerceAtLeast(1e-12)
            val pumped = p / c.launchPrice
            val risk = c.rugRisk * (1 + c.age / 150.0) * (if (pumped > 4) 2.5 else 1.0)
            var rugged = false
            if (Random.nextDouble() < risk) {
                p *= Random.nextDouble(0.005, 0.04)
                rugged = true
                say(rugLine(c))
            }
            coins[i] = c.copy(
                price = p,
                trend = trend,
                history = (c.history + p).takeLast(60),
                age = c.age + 1,
                rugged = rugged,
            )
        }
    }

    fun portfolioValue(): Double = holdings.entries.sumOf { (t, amt) -> amt * (coins.find { it.ticker == t }?.price ?: 0.0) }

    fun buy(ticker: String, fraction: Double) {
        val c = coins.find { it.ticker == ticker } ?: return
        if (c.rugged) return
        val usd = cash * fraction
        if (usd < 0.01) return
        cash -= usd
        holdings[ticker] = (holdings[ticker] ?: 0.0) + usd / c.price
        save()
    }

    fun sell(ticker: String) {
        val c = coins.find { it.ticker == ticker } ?: return
        val amt = holdings.remove(ticker) ?: return
        val usd = amt * c.price * 0.99 // slippage, the dev eats well
        cash += usd
        if (c.rugged) say("you sold your ${c.ticker} for ${"%.2f".format(usd)}. better than nothing i guess")
        save()
    }

    fun bailout() {
        cash = START_CASH
        holdings.clear()
        say("mom sent you another \$1000. she's not happy about it")
        save()
    }

    // ---------- launch your own ----------

    fun startLaunch(name: String, ticker: String) {
        if (cash < LAUNCH_COST || launch?.phase == LaunchPhase.LIVE) return
        cash -= LAUNCH_COST
        val poolTokens = TOTAL_SUPPLY * 0.5
        val p = LAUNCH_COST / poolTokens
        launch = Launch(
            name = name.ifBlank { "Safe Moon Inu" },
            ticker = ticker.ifBlank { "SAFE" }.uppercase().take(6),
            poolUsd = LAUNCH_COST,
            poolTokens = poolTokens,
            devTokens = TOTAL_SUPPLY * 0.5,
            holderTokens = 0.0,
            holders = 0,
            hype = 20.0,
            suspicion = 25.0, // 50% dev wallet. people notice
            locked = false,
            history = List(40) { p },
            seed = LAUNCH_COST,
            spent = 0.0,
            cashedOut = 0.0,
        )
        save()
    }

    fun clearLaunch() {
        launch = null
    }

    private fun Launch.buy(usd: Double): Launch {
        val k = poolUsd * poolTokens
        val newUsd = poolUsd + usd
        val out = poolTokens - k / newUsd
        return copy(poolUsd = newUsd, poolTokens = poolTokens - out, holderTokens = holderTokens + out)
    }

    private fun Launch.sellFromHolders(tokens: Double): Launch {
        val t = tokens.coerceAtMost(holderTokens)
        if (t <= 0) return this
        val k = poolUsd * poolTokens
        val newTokens = poolTokens + t
        return copy(poolTokens = newTokens, poolUsd = k / newTokens, holderTokens = holderTokens - t)
    }

    private fun tickLaunch() {
        var l = launch ?: return
        if (l.phase != LaunchPhase.LIVE) return

        // buyers show up proportional to hype, bigger once there's a crowd
        val buyers = (l.hype / 12 * Random.nextDouble(0.3, 1.6)).toInt()
        repeat(buyers) {
            val usd = Random.nextDouble(15.0, 220.0) * (1 + l.holders / 150.0)
            l = l.buy(usd)
            if (Random.nextDouble() < 0.6) l = l.copy(holders = l.holders + 1)
        }
        // paper hands take profit, more of them when hype is low or it smells off
        val sellChance = (0.15 + (100 - l.hype) / 250 + l.suspicion / 300).coerceAtMost(0.9)
        if (Random.nextDouble() < sellChance && l.holderTokens > 0) {
            l = l.sellFromHolders(l.holderTokens * Random.nextDouble(0.02, 0.12))
            if (Random.nextDouble() < 0.3) l = l.copy(holders = (l.holders - 1).coerceAtLeast(0))
        }

        val pump = l.price / l.history.first()
        val devShare = l.devTokens / TOTAL_SUPPLY
        val susGain = (if (pump > 5) 0.6 else 0.0) + devShare * 0.6
        l = l.copy(
            hype = (l.hype - 1.6).coerceIn(0.0, 100.0),
            suspicion = (l.suspicion - 0.35 + susGain).coerceIn(0.0, 100.0),
            history = (l.history + l.price).takeLast(60),
            ticks = l.ticks + 1,
        )

        if (Random.nextDouble() < 0.025) {
            val ev = Random.nextInt(4)
            l = when (ev) {
                0 -> { say("a whale aped \$2k into \$${l.ticker}"); l.buy(2000.0).copy(holders = l.holders + 1) }
                1 -> { say("someone on reddit called \$${l.ticker} \"the next doge\""); l.copy(hype = l.hype + 15) }
                2 -> { say("zachxbt is looking at \$${l.ticker}... uh oh"); l.copy(suspicion = l.suspicion + 15) }
                else -> { say("a holder posted a 40-tweet thread about \$${l.ticker}'s \"vision\""); l.copy(hype = l.hype + 8, suspicion = (l.suspicion - 5).coerceAtLeast(0.0)) }
            }
        }

        if (l.suspicion >= 100) {
            l = expose(l)
        }
        launch = l
    }

    private fun expose(start: Launch): Launch {
        // everyone dumps first, then you get whatever's left for your bag
        var l = start.sellFromHolders(start.holderTokens)
        val k = l.poolUsd * l.poolTokens
        val newTokens = l.poolTokens + l.devTokens
        val got = l.poolUsd - k / newTokens
        l = l.copy(poolTokens = newTokens, poolUsd = k / newTokens, devTokens = 0.0, cashedOut = l.cashedOut + got)
        cash += got
        val profit = l.cashedOut - l.seed - l.spent
        timesRugged++
        save()
        say("on-chain sleuths doxxed the \$${l.ticker} dev wallet. everyone dumped")
        return l.copy(
            phase = LaunchPhase.EXPOSED,
            history = (l.history + l.price).takeLast(60),
            result = "you got exposed before you could rug. holders dumped on YOU for once.\n" +
                "you scraped out ${money(got)} from your bag.\nnet: ${money(profit)}",
        )
    }

    private fun spend(usd: Double): Boolean {
        if (cash < usd) {
            say("not enough cash. you're broke. the irony")
            return false
        }
        cash -= usd
        launch = launch?.let { it.copy(spent = it.spent + usd) }
        save()
        return true
    }

    private fun live(f: (Launch) -> Launch) {
        val l = launch ?: return
        if (l.phase != LaunchPhase.LIVE) return
        launch = f(l)
    }

    fun shill() = live {
        val gain = 12.0 * (1 - it.hype / 130)
        say(shillLine(it.ticker))
        it.copy(hype = (it.hype + gain).coerceAtMost(100.0), suspicion = it.suspicion + 3.5)
    }

    fun influencer() {
        if (launch?.phase != LaunchPhase.LIVE || !spend(200.0)) return
        live {
            say("a crypto influencer with 900k followers says \$${it.ticker} is \"not financial advice but\"")
            it.copy(hype = (it.hype + 35).coerceAtMost(100.0), suspicion = it.suspicion + 8)
        }
    }

    fun fakeAudit() {
        if (launch?.phase != LaunchPhase.LIVE || !spend(150.0)) return
        live {
            say("\$${it.ticker} passed an audit by \"CertiSafe Labs\" (your cousin)")
            it.copy(suspicion = (it.suspicion - 25).coerceAtLeast(0.0))
        }
    }

    fun lockLiquidity() = live {
        if (it.locked) return@live it
        say("you \"locked\" the liquidity. holders feel safe. you can't pull it now though")
        it.copy(locked = true, suspicion = (it.suspicion - 20).coerceAtLeast(0.0), hype = it.hype + 5)
    }

    fun bots() {
        if (launch?.phase != LaunchPhase.LIVE || !spend(100.0)) return
        live {
            say("300 brand new wallets bought \$${it.ticker}. organic growth")
            var l = it
            repeat(10) { l = l.buy(10.0) }
            l.copy(holders = l.holders + 30, hype = l.hype + 10, suspicion = l.suspicion + 6)
        }
    }

    fun sellSome() = live {
        val t = it.devTokens * 0.1
        if (t <= 0) return@live it
        val k = it.poolUsd * it.poolTokens
        val newTokens = it.poolTokens + t
        val got = it.poolUsd - k / newTokens
        cash += got
        save()
        say("dev wallet sold 10%. the chart noticed. so did twitter")
        it.copy(
            poolTokens = newTokens, poolUsd = k / newTokens, devTokens = it.devTokens - t,
            cashedOut = it.cashedOut + got, suspicion = it.suspicion + 14,
        )
    }

    /** pull liquidity if it's not locked, otherwise dump the dev bag into the pool */
    fun rug() = live {
        var l = it
        val got: Double
        val how: String
        if (!l.locked) {
            got = l.poolUsd
            how = "pulled the liquidity"
            l = l.copy(poolUsd = 0.000001, cashedOut = l.cashedOut + got)
        } else {
            val k = l.poolUsd * l.poolTokens
            val newTokens = l.poolTokens + l.devTokens
            got = l.poolUsd - k / newTokens
            how = "dumped the dev bag"
            l = l.copy(poolTokens = newTokens, poolUsd = k / newTokens, devTokens = 0.0, cashedOut = l.cashedOut + got)
        }
        cash += got
        val profit = l.cashedOut - l.seed - l.spent
        // the bigger the rug, the more people with lawyers you just robbed
        val caught = profit > 2000 && Random.nextDouble() < (profit / 60000).coerceAtMost(0.7)
        var verdict: String
        if (caught) {
            val fine = (cash * 0.8)
            cash -= fine
            verdict = "...and the feds showed up. fined ${money(fine)}. your discord is now evidence."
        } else {
            verdict = verdictFor(profit)
            if (profit > bestRug) bestRug = profit
        }
        timesRugged++
        save()
        say("\$${l.ticker} dev ${how}. ${l.holders} holders are now \"long term investors\"")
        l.copy(
            phase = LaunchPhase.RUGGED,
            history = (l.history + l.price).takeLast(60),
            result = "you $how and walked away with ${money(got)}.\n" +
                "${l.holders} holders left holding the bag.\nnet profit: ${money(profit)}\n\n$verdict",
        )
    }

    private fun verdictFor(p: Double) = when {
        p < 0 -> "you rugged at a loss. that's honestly impressive."
        p < 500 -> "rank: discord mod scammer"
        p < 3000 -> "rank: certified crypto bro"
        p < 15000 -> "rank: lambo (used, leased)"
        p < 60000 -> "rank: dubai penthouse influencer"
        else -> "rank: \"i'm just a builder\" (you are not a builder)"
    }

    fun resetEverything() {
        cash = START_CASH
        holdings.clear()
        launch = null
        say("fresh start. new wallet, who dis")
        save()
    }

    private fun say(s: String) {
        news.add(0, s)
        while (news.size > 30) news.removeAt(news.size - 1)
    }

    private fun save() {
        prefs.edit()
            .putString("cash", cash.toString())
            .putString("best", bestRug.toString())
            .putInt("rugs", timesRugged)
            .apply()
    }

    override fun onCleared() {
        // whatever your bags were worth when you left is what you get back next time
        cash += portfolioValue()
        holdings.clear()
        // a live launch you walk away from gets panic-sold at half value
        launch?.let { if (it.phase == LaunchPhase.LIVE) cash += it.devValue * 0.5 }
        save()
    }
}

private val prefixes = listOf("Doge", "Pepe", "Shiba", "Elon", "Moon", "Safe", "Baby", "Floki", "Turbo", "Bonk", "Giga", "Chad", "Wojak", "Rocket", "Hamster", "Banana", "Skibidi", "Sigma", "Rizz", "Kitty")
private val suffixes = listOf("Inu", "Coin", "Moon", "Rocket", "AI", "Mars", "Cash", "Swap", "X", "Verse", "Gold", "Fi", "Pump", "Lambo", "420")

private fun randomName(): Pair<String, String> {
    val a = prefixes.random()
    val b = suffixes.random()
    val ticker = (a.take(Random.nextInt(2, 4)) + b.take(Random.nextInt(1, 3))).uppercase()
    return ticker to "$a $b"
}

private fun pumpLine(t: String) = listOf(
    "elon posted a dog wearing a \$$t hat. it's pumping",
    "\$$t got listed on an exchange nobody's heard of. +40%",
    "a 14 year old on tiktok says \$$t is going to 1 dollar",
    "\$$t trending on crypto twitter. lambo emojis everywhere",
).random()

private fun fudLine(t: String) = listOf(
    "rumor: \$$t devs are \"on vacation\". indefinitely",
    "\$$t's website went down. it was a canva template anyway",
    "a big holder of \$$t is moving coins to an exchange",
    "the \$$t telegram group got muted by admins. hmm",
).random()

private fun rugLine(c: Coin) = listOf(
    "\$${c.ticker} devs pulled the liquidity. -98%. classic",
    "${c.name} team deleted their twitter. \$${c.ticker} is a ghost town",
    "\$${c.ticker} \"migrated to a new contract\". the old one is worth 0",
    "the ${c.name} dev just bought a lambo. \$${c.ticker} -97%",
).random()

private fun shillLine(t: String) = listOf(
    "you replied \"\$$t 100x gem\" under 40 elon tweets",
    "you posted rocket emojis in 12 telegram groups",
    "you made a \$$t meme. it's mid but it's working",
    "you told your discord \"we're so early\"",
    "you posted a chart with a line going up. any line",
).random()

fun fmtPrice(p: Double): String = when {
    p >= 100 -> "%.2f".format(p)
    p >= 1 -> "%.3f".format(p)
    p >= 0.01 -> "%.4f".format(p)
    p == 0.0 -> "0"
    else -> {
        // 0.0000041234 -> 0.0₅41234 is cute but unreadable for most, just show sig digits
        "%.3e".format(p)
    }
}

fun money(v: Double) = com.simulasim.app.ui.money(v)
