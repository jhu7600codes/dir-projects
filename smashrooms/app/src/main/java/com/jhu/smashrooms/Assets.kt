package com.jhu.smashrooms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/** every image, sound and the font, all straight out of the-rooms build (see tools/import_the_rooms.py) */
object Assets {
    private lateinit var ctx: Context
    private val bmp = HashMap<String, Bitmap>()
    lateinit var caveat: Typeface
    val mono: Typeface by lazy { Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }

    private lateinit var pool: SoundPool
    private val ids = HashMap<String, Int>()
    private var music: MediaPlayer? = null
    private var musicName: String? = null
    private var musicVol = 0.6f
    private var paused = false

    var sfxOn = true
    var musicOn = true
        set(v) {
            field = v
            val n = musicName
            musicName = null
            music?.release(); music = null
            if (v && n != null) music(n, musicVol)
            else musicName = n
        }

    fun load(c: Context) {
        if (::ctx.isInitialized) return
        ctx = c.applicationContext
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        for (dir in listOf("img", "bg")) {
            for (f in ctx.assets.list(dir).orEmpty()) {
                ctx.assets.open("$dir/$f").use { s ->
                    BitmapFactory.decodeStream(s, null, opts)?.let { bmp[f.substringBeforeLast('.')] = it }
                }
            }
        }
        caveat = Typeface.createFromAsset(ctx.assets, "font/caveat.ttf")
        pool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        for (f in ctx.assets.list("sfx").orEmpty()) {
            ctx.assets.openFd("sfx/$f").use { ids[f.substringBeforeLast('.')] = pool.load(it, 1) }
        }
    }

    fun img(name: String): Bitmap = bmp[name] ?: bmp.values.first()

    fun play(name: String, vol: Float = 1f, rate: Float = 1f): Int {
        if (!sfxOn) return 0
        val id = ids[name] ?: return 0
        val v = vol.coerceIn(0f, 1f)
        return pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun stop(stream: Int) {
        if (stream != 0) pool.stop(stream)
    }

    fun music(name: String?, vol: Float = 0.6f) {
        if (name == musicName && music != null) return
        music?.release()
        music = null
        musicName = name
        musicVol = vol
        if (name == null || !musicOn) return
        try {
            val mp = MediaPlayer()
            ctx.assets.openFd("music/$name.ogg").use { fd -> mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            mp.isLooping = true
            mp.setVolume(vol, vol)
            mp.prepare()
            if (!paused) mp.start()
            music = mp
        } catch (_: Exception) {
            music = null
        }
    }

    fun pause() {
        paused = true
        music?.takeIf { it.isPlaying }?.pause()
        pool.autoPause()
    }

    fun resume() {
        paused = false
        music?.start()
        pool.autoResume()
    }
}
