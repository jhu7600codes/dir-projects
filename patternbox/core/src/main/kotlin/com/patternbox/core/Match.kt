package com.patternbox.core

import kotlin.random.Random

/** a fighter taking part in a match */
data class Entry(val name: String, val loadout: Loadout, val rosterId: Long = -1)

enum class Mode(val label: String, val blurb: String, val minFighters: Int, val maxFighters: Int) {
    VERSUS("1v1", "two fighters, one survives", 2, 2),
    TEAMS("2v2", "two teams of two", 4, 4),
    FFA("free for all", "up to six fighters, everyone vs everyone", 3, 6),
    SURVIVAL("survival", "you vs endless waves of random fighters", 1, 1),
    TOURNAMENT("tournament", "8 fighter bracket, watch or simulate each match", 2, 8),
    SANDBOX("sandbox", "no rules. spawn stuff, blow stuff up", 1, 6),
}

/**
 * runs a mode on top of an arena: who's on which team, survival waves, results.
 * the arena does the physics, this does the rules.
 */
class Match(val mode: Mode, val entries: List<Entry>, val map: GameMap, val arena: Arena = Arena()) {
    var wave = 0; private set
    private var waveDelay = 0f
    var bestWave = 0; private set
    val over get() = mode != Mode.SANDBOX && arena.winnerTeam != null && (mode != Mode.SURVIVAL || player?.dead == true)
    val player get() = arena.fighters.firstOrNull { it.team == 0 }

    fun start() {
        arena.endless = mode == Mode.SANDBOX
        arena.reset(map)
        when (mode) {
            Mode.VERSUS, Mode.TOURNAMENT -> {
                arena.add(entries[0].loadout, entries[0].name, 0, arena.width * .25f)
                arena.add(entries[1].loadout, entries[1].name, 1, arena.width * .75f)
            }
            Mode.TEAMS -> entries.forEachIndexed { i, e ->
                val left = i % 2 == 0
                arena.add(e.loadout, e.name, if (left) 0 else 1, arena.width * (if (left) .15f + i * .05f else .85f - i * .05f))
            }
            Mode.FFA, Mode.SANDBOX -> entries.forEachIndexed { i, e ->
                arena.add(e.loadout, e.name, i, arena.width * ((i + .5f) / entries.size))
            }
            Mode.SURVIVAL -> {
                arena.add(entries[0].loadout, entries[0].name, 0, arena.width * .5f)
                nextWave()
            }
        }
    }

    private fun nextWave() {
        wave++
        val n = minOf(1 + (wave - 1) / 2, 4)
        val hpMult = .55f + wave * .12f
        repeat(n) { k ->
            val l = Loadout.random()
            val left = k % 2 == 0
            arena.add(l, l.autoName, 1, if (left) arena.width * .08f else arena.width * .92f, hpMult).also {
                it.y = 60f + k * 20
            }
        }
        arena.texts += FloatText(arena.width / 2, 90f, "wave $wave", FloatText.Tone.CRIT, 2f)
        arena.clearWinner()
    }

    fun step(dt: Float = 1f / 60f) {
        arena.step(dt)
        if (mode == Mode.SURVIVAL) {
            val p = player ?: return
            if (p.dead) { bestWave = wave - 1; return }
            val enemiesAlive = arena.fighters.any { it.team == 1 && !it.dead }
            if (!enemiesAlive) {
                if (waveDelay <= 0f) waveDelay = 1.6f
                waveDelay -= dt
                if (waveDelay <= 0f) {
                    // clear out the dead so the list doesn't grow forever, heal a bit between waves
                    arena.fighters.removeAll { it.dead && it.team == 1 }
                    p.hp = minOf(p.maxHp, p.hp + p.maxHp * .25f)
                    p.plates.fill(true)
                    nextWave()
                }
            }
        }
    }

    /** index into entries of each fighter that won (for records) */
    fun winners(): List<Int> {
        val t = arena.winnerTeam ?: return emptyList()
        return when (mode) {
            Mode.VERSUS, Mode.TOURNAMENT -> listOf(t)
            Mode.TEAMS -> entries.indices.filter { (if (it % 2 == 0) 0 else 1) == t }
            Mode.FFA -> listOf(t)
            else -> emptyList()
        }
    }

    companion object {
        /** play a 1v1 with bots and no rendering. returns 0 or 1 */
        fun simulate(a: Entry, b: Entry, map: GameMap, maxSeconds: Int = 120, rnd: Random = Random.Default): Int {
            val m = Match(Mode.VERSUS, listOf(a, b), map)
            m.start()
            var ticks = 0
            while (m.arena.winnerTeam == null && ticks < maxSeconds * 60) { m.step(); ticks++ }
            val w = m.arena.winnerTeam
            if (w == 0 || w == 1) return w
            // timeout or double ko: whoever has more hp left, coin flip on ties
            val f = m.arena.fighters
            val p0 = f[0].hp / f[0].maxHp
            val p1 = f[1].hp / f[1].maxHp
            return if (p0 == p1) rnd.nextInt(2) else if (p0 > p1) 0 else 1
        }
    }
}

/** single elimination bracket of 4 or 8. missing spots get filled with presets */
class Tournament(entries: List<Entry>, val map: GameMap, val size: Int = 8) {
    val rounds = mutableListOf<MutableList<Int?>>()   // indices into players, null = not decided yet
    val players: List<Entry>

    init {
        require(size == 4 || size == 8)
        val list = entries.take(size).toMutableList()
        while (list.size < size) {
            val p = Presets.all.random()
            if (list.none { it.name == p.first }) list += Entry(p.first, p.second)
            else Loadout.random().let { list += Entry(it.autoName, it) }
        }
        players = list.shuffled()
        rounds += players.indices.toMutableList<Int?>()
        var n = size / 2
        while (n >= 1) { rounds += MutableList<Int?>(n) { null }; n /= 2 }
    }

    val champion: Entry? get() = rounds.last()[0]?.let { players[it] }
    /** set by the app once the champion's cup is saved, so it only happens once */
    var cupAwarded = false

    /** (round, match index) of the next match to play, null when done */
    fun next(): Pair<Int, Int>? {
        for (r in 0 until rounds.size - 1) {
            val nextRound = rounds[r + 1]
            for (m in nextRound.indices) if (nextRound[m] == null && rounds[r][m * 2] != null && rounds[r][m * 2 + 1] != null) return r to m
        }
        return null
    }

    fun pair(round: Int, match: Int): Pair<Entry, Entry> =
        players[rounds[round][match * 2]!!] to players[rounds[round][match * 2 + 1]!!]

    /** winner is 0 (first of the pair) or 1 */
    fun record(round: Int, match: Int, winner: Int) {
        rounds[round + 1][match] = rounds[round][match * 2 + winner]
    }

    fun simulateNext(): Boolean {
        val (r, m) = next() ?: return false
        val (a, b) = pair(r, m)
        record(r, m, Match.simulate(a, b, map))
        return true
    }

    fun roundName(r: Int): String = when (rounds.size - 1 - r) {
        1 -> "final"
        2 -> "semis"
        3 -> "quarters"
        else -> "round ${r + 1}"
    }
}
