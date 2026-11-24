package com.patternbox.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.patternbox.core.Event
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** all sounds are made from maths at startup, no audio files in the apk */
class Sfx(context: Context) : Feedback {
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = mutableMapOf<String, Int>()
    private var lastPlay = mutableMapOf<String, Long>()

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        val rate = 22050
        fun make(name: String, seconds: Float, gen: (t: Float, i: Int) -> Float) {
            val n = (rate * seconds).toInt()
            val samples = ShortArray(n) { i -> (gen(i / rate.toFloat(), i).coerceIn(-1f, 1f) * 32000).toInt().toShort() }
            val f = File(dir, "$name.wav")
            writeWav(f, samples, rate)
            ids[name] = pool.load(f.path, 1)
        }
        val rnd = Random(7)
        make("hit", .12f) { t, _ -> (rnd.nextFloat() * 2 - 1) * exp(-t * 40) * .8f + sin(2 * PI * 140 * t).toFloat() * exp(-t * 30) }
        make("crit", .2f) { t, _ -> (rnd.nextFloat() * 2 - 1) * exp(-t * 20) * .7f + sin(2 * PI * 90 * t).toFloat() * exp(-t * 14) }
        make("clang", .35f) { t, _ -> (sin(2 * PI * 1250 * t) + .6 * sin(2 * PI * 1870 * t) + .4 * sin(2 * PI * 2630 * t)).toFloat() * exp(-t * 11) * .45f }
        make("boom", .7f) { t, _ -> (rnd.nextFloat() * 2 - 1) * exp(-t * 6) * .9f + sin(2 * PI * (60 - t * 40) * t).toFloat() * exp(-t * 5) * .8f }
        make("ko", .6f) { t, _ -> sin(2 * PI * (520 - t * 600) * t).toFloat() * exp(-t * 3) * .5f }
        make("shoot", .12f) { t, _ -> sin(2 * PI * (900 - t * 3000) * t).toFloat() * exp(-t * 25) * .5f }
        make("jump", .1f) { t, _ -> sin(2 * PI * (300 + t * 2500) * t).toFloat() * exp(-t * 25) * .25f }
        make("bounce", .25f) { t, _ -> sin(2 * PI * (200 + sin(t * 60) * 80) * t).toFloat() * exp(-t * 8) * .5f }
        make("break", .2f) { t, _ -> (rnd.nextFloat() * 2 - 1) * exp(-t * 18) * (if ((t * 90).toInt() % 2 == 0) .8f else .3f) }
        make("ability", .3f) { t, _ -> sin(2 * PI * (440 + t * 1200) * t).toFloat() * exp(-t * 6) * .35f }
        make("win", 1f) { t, _ ->
            val note = when { t < .15f -> 523f; t < .3f -> 659f; t < .45f -> 784f; else -> 1046f }
            sin(2 * PI * note * t).toFloat() * (if (t < .45f) .35f else .35f * exp(-(t - .45f) * 3))
        }
    }

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator

    override fun buzz(big: Boolean) {
        val v = vibrator ?: return
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                v.vibrate(android.os.VibrationEffect.createPredefined(if (big) android.os.VibrationEffect.EFFECT_HEAVY_CLICK else android.os.VibrationEffect.EFFECT_TICK))
            } else {
                v.vibrate(android.os.VibrationEffect.createOneShot(if (big) 40L else 12L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    override fun play(e: Event) {
        val name = when (e) {
            is Event.Hit -> if (e.crit) "crit" else "hit"
            Event.Clang -> "clang"
            is Event.Boom -> "boom"
            Event.Ko -> "ko"
            Event.Shoot -> "shoot"
            Event.Jump -> "jump"
            Event.Bounce -> "bounce"
            Event.Break -> "break"
            Event.Ability -> "ability"
            Event.Win -> "win"
        }
        // don't machine gun the same sound
        val now = System.currentTimeMillis()
        if (now - (lastPlay[name] ?: 0) < 45) return
        lastPlay[name] = now
        val vol = if (e is Event.Jump) .5f else 1f
        ids[name]?.let { pool.play(it, vol, vol, 1, 0, .9f + Random.nextFloat() * .2f) }
    }

    fun release() = pool.release()

    private fun writeWav(f: File, s: ShortArray, rate: Int) {
        val data = ByteBuffer.allocate(s.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        s.forEach { data.putShort(it) }
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()); h.putInt(36 + s.size * 2); h.put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()); h.putInt(16); h.putShort(1); h.putShort(1); h.putInt(rate); h.putInt(rate * 2); h.putShort(2); h.putShort(16)
        h.put("data".toByteArray()); h.putInt(s.size * 2)
        FileOutputStream(f).use { it.write(h.array()); it.write(data.array()) }
    }
}
