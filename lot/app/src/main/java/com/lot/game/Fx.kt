package com.lot.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { CLICK, COIN, WIN, BIG_WIN, LOSE, SCRATCH, BALL, YELL, ANGRY, LAUGH, CAUGHT, SNEAK, BADGE, BUY, DRUM, WHOOSH }
enum class Buzz { TICK, LIGHT, MEDIUM, HEAVY, SUCCESS, FAIL }

val LocalFx = staticCompositionLocalOf<Fx> { error("no fx") }

/**
 * all sounds are synthesized at runtime (tiny chiptune style), so the apk ships no audio files.
 */
class Fx(context: Context) {
    @Volatile var soundOn = true
    @Volatile var hapticsOn = true

    private val rate = 22050
    private val pool = Executors.newCachedThreadPool()
    private val cache = HashMap<Sfx, ShortArray>()
    private val lastPlay = HashMap<Sfx, Long>()

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (e: Exception) {
        null
    }

    fun play(s: Sfx) {
        if (!soundOn) return
        val now = System.currentTimeMillis()
        val minGap = when (s) {
            Sfx.SCRATCH -> 90L
            Sfx.BALL -> 60L
            else -> 30L
        }
        synchronized(lastPlay) {
            if (now - (lastPlay[s] ?: 0L) < minGap) return
            lastPlay[s] = now
        }
        pool.execute {
            try {
                val pcm = synchronized(cache) { cache.getOrPut(s) { synth(s) } }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / rate + 80)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    fun buzz(b: Buzz) {
        if (!hapticsOn) return
        val v = vibrator ?: return
        try {
            val effect = when (b) {
                Buzz.TICK -> VibrationEffect.createOneShot(8, 60)
                Buzz.LIGHT -> VibrationEffect.createOneShot(18, 90)
                Buzz.MEDIUM -> VibrationEffect.createOneShot(35, 160)
                Buzz.HEAVY -> VibrationEffect.createOneShot(70, 255)
                Buzz.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30, 60, 60), intArrayOf(0, 180, 0, 200, 0, 255), -1)
                Buzz.FAIL -> VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 220), intArrayOf(0, 255, 0, 200), -1)
            }
            v.vibrate(effect)
        } catch (_: Exception) {
        }
    }

    // ---------------- synth ----------------

    private enum class Wave { SINE, SQUARE, SAW, TRI, NOISE }

    private class Buf {
        val data = ArrayList<Float>()
    }

    private fun Buf.tone(
        f0: Float, f1: Float, ms: Int, wave: Wave, vol: Float = 0.5f,
        vibrato: Float = 0f, attack: Float = 0.01f,
    ) {
        val n = rate * ms / 1000
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / n
            var f = f0 + (f1 - f0) * t
            if (vibrato > 0f) f *= 1f + vibrato * sin(2 * PI * 7 * i / rate).toFloat()
            phase += f / rate
            val p = (phase % 1.0).toFloat()
            val s = when (wave) {
                Wave.SINE -> sin(2 * PI * p).toFloat()
                Wave.SQUARE -> if (p < 0.5f) 0.6f else -0.6f
                Wave.SAW -> (2f * p - 1f) * 0.7f
                Wave.TRI -> (if (p < 0.5f) 4f * p - 1f else 3f - 4f * p)
                Wave.NOISE -> Random.nextFloat() * 2f - 1f
            }
            val atk = (t / attack).coerceAtMost(1f)
            val env = atk * (1f - t) * (1f - t).coerceAtLeast(0f) + atk * 0.15f * (1f - t)
            data.add(s * vol * env)
        }
    }

    private fun Buf.rest(ms: Int) {
        repeat(rate * ms / 1000) { data.add(0f) }
    }

    private fun synth(s: Sfx): ShortArray {
        val b = Buf()
        when (s) {
            Sfx.CLICK -> b.tone(900f, 700f, 35, Wave.SQUARE, 0.25f)
            Sfx.COIN -> { b.tone(988f, 988f, 60, Wave.SQUARE, 0.35f); b.tone(1319f, 1319f, 220, Wave.SQUARE, 0.35f) }
            Sfx.WIN -> listOf(523f, 659f, 784f, 1047f).forEach { b.tone(it, it, 110, Wave.SQUARE, 0.35f) }
            Sfx.BIG_WIN -> {
                repeat(2) { listOf(523f, 659f, 784f, 1047f).forEach { b.tone(it, it, 80, Wave.SQUARE, 0.35f) } }
                b.tone(1047f, 1047f, 120, Wave.SQUARE, 0.35f); b.tone(1319f, 1319f, 120, Wave.SQUARE, 0.35f)
                b.tone(1568f, 1568f, 400, Wave.SQUARE, 0.35f, vibrato = 0.01f)
            }
            Sfx.LOSE -> {
                // the classic sad trombone
                b.tone(392f, 380f, 260, Wave.SAW, 0.4f); b.tone(370f, 358f, 260, Wave.SAW, 0.4f)
                b.tone(349f, 338f, 260, Wave.SAW, 0.4f); b.tone(330f, 300f, 700, Wave.SAW, 0.4f, vibrato = 0.03f)
            }
            Sfx.SCRATCH -> b.tone(0f, 0f, 45, Wave.NOISE, 0.18f)
            Sfx.BALL -> b.tone(700f, 300f, 60, Wave.SINE, 0.5f)
            Sfx.YELL -> b.tone(220f, 180f, 380, Wave.SAW, 0.45f, vibrato = 0.06f)
            Sfx.ANGRY -> { b.tone(140f, 110f, 200, Wave.SAW, 0.5f, vibrato = 0.1f); b.tone(120f, 90f, 260, Wave.SAW, 0.5f, vibrato = 0.1f) }
            Sfx.LAUGH -> repeat(4) { b.tone(520f, 440f, 70, Wave.TRI, 0.4f); b.rest(40) }
            Sfx.CAUGHT -> repeat(3) { b.tone(700f, 700f, 150, Wave.SQUARE, 0.35f); b.tone(950f, 950f, 150, Wave.SQUARE, 0.35f) }
            Sfx.SNEAK -> { b.tone(196f, 196f, 70, Wave.TRI, 0.45f); b.rest(50); b.tone(247f, 247f, 70, Wave.TRI, 0.45f) }
            Sfx.BADGE -> {
                b.tone(784f, 784f, 90, Wave.SQUARE, 0.3f); b.tone(784f, 784f, 90, Wave.SQUARE, 0.3f)
                b.tone(784f, 784f, 90, Wave.SQUARE, 0.3f); b.tone(1047f, 1047f, 380, Wave.SQUARE, 0.3f, vibrato = 0.01f)
            }
            Sfx.BUY -> { b.tone(0f, 0f, 60, Wave.NOISE, 0.25f); b.tone(2093f, 2093f, 260, Wave.SINE, 0.4f) }
            Sfx.DRUM -> { repeat(14) { b.tone(0f, 0f, 35, Wave.NOISE, 0.15f + it * 0.015f) }; b.tone(0f, 0f, 160, Wave.NOISE, 0.5f) }
            Sfx.WHOOSH -> b.tone(0f, 0f, 220, Wave.NOISE, 0.2f, attack = 0.5f)
        }
        return ShortArray(b.data.size) { (b.data[it].coerceIn(-1f, 1f) * 32000).toInt().toShort() }
    }

    // ---------------- background music ----------------

    @Volatile private var musicWanted = false
    @Volatile private var visible = true
    private var musicTrack: AudioTrack? = null
    private val musicLock = Any()

    fun setMusic(on: Boolean) {
        musicWanted = on
        refreshMusic()
    }

    fun setVisible(v: Boolean) {
        visible = v
        refreshMusic()
    }

    private fun refreshMusic() {
        pool.execute {
            synchronized(musicLock) {
                try {
                    if (musicWanted && visible) {
                        val t = musicTrack ?: buildMusic().also { musicTrack = it }
                        if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
                    } else {
                        musicTrack?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    /** a little 8 bar chiptune loop: C, Am, F, G with bass, arpeggio, lead and hats */
    private fun buildMusic(): AudioTrack {
        val bpm = 112f
        val step = 60f / bpm / 2f // eighth notes
        val steps = 64
        val n = (steps * step * rate).toInt()
        val out = FloatArray(n)
        fun midi(m: Int) = 440f * Math.pow(2.0, (m - 69) / 12.0).toFloat()
        fun note(start: Int, len: Int, freq: Float, vol: Float, wave: Wave) {
            val s0 = (start * step * rate).toInt()
            val s1 = minOf(n, ((start + len) * step * rate).toInt())
            var phase = 0.0
            for (i in s0 until s1) {
                val t = (i - s0).toFloat() / (s1 - s0)
                phase += freq / rate
                val p = (phase % 1.0).toFloat()
                val v = when (wave) {
                    Wave.SQUARE -> if (p < 0.25f) 0.6f else -0.6f
                    Wave.TRI -> if (p < 0.5f) 4f * p - 1f else 3f - 4f * p
                    Wave.NOISE -> Random.nextFloat() * 2f - 1f
                    else -> sin(2 * PI * p).toFloat()
                }
                val env = (1f - t) * minOf(1f, t * 60f)
                out[i] += v * vol * env
            }
        }
        val chords = listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))
        val lead = listOf(
            76, -1, 79, 76, 74, 72, -1, 72, 76, -1, 74, 72, 69, -1, 67, -1,
            72, -1, 76, 72, 69, 67, -1, 69, 72, -1, 69, 67, 64, -1, -1, -1,
            65, -1, 69, 72, 74, 72, -1, 69, 72, -1, 69, 65, 64, -1, 65, -1,
            67, -1, 71, 74, 79, 77, -1, 74, 72, -1, 71, -1, 67, -1, -1, -1,
        )
        for (st in 0 until steps) {
            val ch = chords[(st / 16) % 4]
            if (st % 2 == 0) note(st, 2, midi(ch[0] - 24), 0.13f, Wave.SQUARE)
            note(st, 1, midi(ch[st % 3] + 12), 0.05f, Wave.TRI)
            if (lead[st] > 0) note(st, 2, midi(lead[st]), 0.07f, Wave.SQUARE)
            if (st % 2 == 1) note(st, 1, 0f, 0.025f, Wave.NOISE)
        }
        val pcm = ShortArray(n) { (out[it].coerceIn(-1f, 1f) * 20000).toInt().toShort() }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        track.setVolume(0.6f)
        return track
    }
}
