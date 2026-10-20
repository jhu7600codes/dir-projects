package com.jhu.smashrooms

import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/**
 * no emulator needed: renders real frames of every screen to app/build/screens/ and
 * runs a pile of cpu-only fights to make sure nothing crashes or flies off to NaN.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GameTest {
    private val out = File("build/screens").apply { mkdirs() }

    @Before
    fun load() {
        Assets.load(RuntimeEnvironment.getApplication())
        Assets.musicOn = false
    }

    private fun view(): GameView = GameView(RuntimeEnvironment.getApplication()).also { it.w = 1600f; it.h = 720f }

    private fun shot(s: Screen, name: String) {
        val bmp = Bitmap.createBitmap(1600, 720, Bitmap.Config.ARGB_8888)
        s.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun drive(b: Battle, frames: Int, ai: Ai, pad: Pad) {
        repeat(frames) {
            pad.clear()
            if (b.countdown == 0) ai.think(b.p1, b, pad)
            b.update(pad)
            for (f in b.fighters) {
                assertTrue("${f.def.id} x is ${f.x}", f.x.isFinite() && f.y.isFinite())
                assertTrue("${f.def.id} v", f.vx.isFinite() && f.vy.isFinite())
            }
        }
    }

    @Test
    fun menus() {
        val g = view()
        shot(TitleScreen(g), "01_title")
        shot(SelectScreen(g), "02_select")
        g.settings.cpus = mutableListOf(4, -1, 9)
        shot(SelectScreen(g), "03_select_4p")
        shot(StageScreen(g), "04_stages")
        for (i in Roster.ALL.indices step 5) shot(RosterScreen(g, i), "05_roster_${Roster.ALL[i].id}")
        shot(CreditsScreen(g), "06_credits")
    }

    @Test
    fun battleFrames() {
        val g = view()
        g.settings.p1 = 6
        g.settings.cpus = mutableListOf(4, 8, 13)
        g.settings.level = 9
        for (st in listOf(0, 2, 6)) {
            g.settings.stage = st
            val screen = BattleScreen(g)
            val field = BattleScreen::class.java.getDeclaredField("b").apply { isAccessible = true }
            val b = field.get(screen) as Battle
            shot(screen, "10_stage${st}_countdown")
            val ai = Ai(9, false)
            val pad = Pad()
            for (k in 1..4) {
                repeat(150) {
                    pad.clear()
                    if (b.countdown == 0) ai.think(b.p1, b, pad)
                    screen.updateWith(pad)
                }
                shot(screen, "11_stage${st}_fight$k")
            }
            if (st == 0) {
                // show every super going off at once
                for (f in b.fighters) if (!f.dead) { f.fear = 100f; f.start(f.def.sup, b) }
                repeat(40) { screen.updateWith(Pad()) }
                shot(screen, "12_supers")
            }
        }
    }

    @Test
    fun everyMoveRuns() {
        val set = Settings(stage = 1, cpus = mutableListOf(0), items = false)
        for ((i, d) in Roster.ALL.withIndex()) {
            val moves = d.specials + d.sup + listOf(
                d.normals.jab, d.normals.ftilt, d.normals.utilt, d.normals.dtilt, d.normals.fsmash, d.normals.usmash,
                d.normals.dsmash, d.normals.nair, d.normals.fair, d.normals.bair, d.normals.uair, d.normals.dair,
            )
            for (m in moves) {
                set.p1 = i
                set.cpus = mutableListOf((i + 3) % Roster.ALL.size)
                val b = Battle(set)
                repeat(200) { b.update(Pad()) }
                val f = b.p1
                val foe = b.fighters[1]
                foe.x = f.x + 120f
                f.facing = 1
                f.start(m, b)
                repeat(400) {
                    b.update(Pad())
                    assertTrue("${d.id} ${m.name}", f.x.isFinite() && f.y.isFinite() && foe.x.isFinite())
                }
            }
        }
    }

    @Test
    fun cpuBattles() {
        val rnd = Random(7)
        var finished = 0
        repeat(24) { n ->
            val set = Settings(
                p1 = rnd.nextInt(Roster.ALL.size),
                cpus = MutableList(1 + rnd.nextInt(3)) { rnd.nextInt(Roster.ALL.size) },
                level = 1 + rnd.nextInt(9),
                stocks = 2,
                stage = n % Stage.ALL.size,
            )
            val b = Battle(set)
            drive(b, 60 * 60 * 4, Ai(set.level, false), Pad())
            if (b.over > 0) finished++
            println("battle $n: ${b.fighters.joinToString { "${it.def.id} s${it.stocks} ko${it.kos} ${it.dmg.toInt()}%" }} over=${b.over}")
        }
        println("finished $finished / 24")
        assertTrue("cpus should actually finish fights", finished >= 12)
    }
}
