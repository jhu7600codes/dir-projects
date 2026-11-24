package com.patternbox.desktop

import com.patternbox.app.Feedback
import com.patternbox.app.Store
import com.patternbox.core.Event
import java.io.File
import java.util.Properties
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** the save file. a plain properties file, writes are flushed right away */
class PropertiesStore(private val file: File) : Store {
    private val p = Properties().apply { if (file.exists()) runCatching { file.inputStream().use { load(it) } } }
    private val SEP = '\u0001'

    @Synchronized private fun save() {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.outputStream().use { p.store(it, "patternbox save") }
            tmp.renameTo(file)
        }
    }

    override fun getString(key: String, def: String?): String? = p.getProperty(key) ?: def
    override fun getBoolean(key: String, def: Boolean) = p.getProperty(key)?.toBooleanStrictOrNull() ?: def
    override fun getInt(key: String, def: Int) = p.getProperty(key)?.toIntOrNull() ?: def
    override fun getLong(key: String, def: Long) = p.getProperty(key)?.toLongOrNull() ?: def
    override fun getFloat(key: String, def: Float) = p.getProperty(key)?.toFloatOrNull() ?: def
    override fun getStringSet(key: String, def: Set<String>?): Set<String>? =
        p.getProperty(key)?.let { v -> if (v.isEmpty()) emptySet() else v.split(SEP).toSet() } ?: def

    override fun edit(): Store.Editor = object : Store.Editor {
        val changes = mutableMapOf<String, String?>()
        override fun putString(key: String, v: String?) = apply { changes[key] = v }
        override fun putBoolean(key: String, v: Boolean) = apply { changes[key] = v.toString() }
        override fun putInt(key: String, v: Int) = apply { changes[key] = v.toString() }
        override fun putLong(key: String, v: Long) = apply { changes[key] = v.toString() }
        override fun putFloat(key: String, v: Float) = apply { changes[key] = v.toString() }
        override fun putStringSet(key: String, v: Set<String>?) = apply { changes[key] = v?.joinToString(SEP.toString()) }
        override fun apply() {
            synchronized(this@PropertiesStore) {
                for ((k, v) in changes) if (v == null) p.remove(k) else p.setProperty(k, v)
            }
            save()
        }
    }
}

/** the same maths-made sounds as the phone, through java sound. silent if there's no audio device */
class DesktopSfx : Feedback {
    private val rate = 22050f
    private val clips = mutableMapOf<String, List<Clip>>()
    private val next = mutableMapOf<String, Int>()
    private var lastPlay = mutableMapOf<String, Long>()

    init {
        runCatching {
            val rnd = Random(7)
            fun make(name: String, seconds: Float, gen: (t: Float) -> Float) {
                val n = (rate * seconds).toInt()
                val bytes = ByteArray(n * 2)
                for (i in 0 until n) {
                    val v = (gen(i / rate).coerceIn(-1f, 1f) * 32000).toInt()
                    bytes[i * 2] = (v and 0xff).toByte(); bytes[i * 2 + 1] = (v shr 8).toByte()
                }
                val fmt = AudioFormat(rate, 16, 1, true, false)
                // a few copies so the same sound can overlap
                clips[name] = List(3) { AudioSystem.getClip().also { it.open(fmt, bytes, 0, bytes.size) } }
            }
            make("hit", .12f) { t -> (rnd.nextFloat() * 2 - 1) * exp(-t * 40) * .8f + sin(2 * PI * 140 * t).toFloat() * exp(-t * 30) }
            make("crit", .2f) { t -> (rnd.nextFloat() * 2 - 1) * exp(-t * 20) * .7f + sin(2 * PI * 90 * t).toFloat() * exp(-t * 14) }
            make("clang", .35f) { t -> (sin(2 * PI * 1250 * t) + .6 * sin(2 * PI * 1870 * t) + .4 * sin(2 * PI * 2630 * t)).toFloat() * exp(-t * 11) * .45f }
            make("boom", .7f) { t -> (rnd.nextFloat() * 2 - 1) * exp(-t * 6) * .9f + sin(2 * PI * (60 - t * 40) * t).toFloat() * exp(-t * 5) * .8f }
            make("ko", .6f) { t -> sin(2 * PI * (520 - t * 600) * t).toFloat() * exp(-t * 3) * .5f }
            make("shoot", .12f) { t -> sin(2 * PI * (900 - t * 3000) * t).toFloat() * exp(-t * 25) * .5f }
            make("jump", .1f) { t -> sin(2 * PI * (300 + t * 2500) * t).toFloat() * exp(-t * 25) * .2f }
            make("bounce", .25f) { t -> sin(2 * PI * (200 + sin(t * 60) * 80) * t).toFloat() * exp(-t * 8) * .5f }
            make("break", .2f) { t -> (rnd.nextFloat() * 2 - 1) * exp(-t * 18) * (if ((t * 90).toInt() % 2 == 0) .8f else .3f) }
            make("ability", .3f) { t -> sin(2 * PI * (440 + t * 1200) * t).toFloat() * exp(-t * 6) * .35f }
            make("win", 1f) { t ->
                val note = when { t < .15f -> 523f; t < .3f -> 659f; t < .45f -> 784f; else -> 1046f }
                sin(2 * PI * note * t).toFloat() * (if (t < .45f) .35f else .35f * exp(-(t - .45f) * 3))
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
        val now = System.currentTimeMillis()
        if (now - (lastPlay[name] ?: 0) < 45) return
        lastPlay[name] = now
        val list = clips[name] ?: return
        val i = (next[name] ?: 0) % list.size
        next[name] = i + 1
        runCatching { list[i].apply { stop(); framePosition = 0; start() } }
    }

    fun close() = clips.values.flatten().forEach { runCatching { it.close() } }
}
