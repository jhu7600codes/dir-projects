package app.krala.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Records one utterance to a small m4a for whisper. Stops by itself after ~1.4 s of quiet once
 * speech was heard, or after 60 s.
 */
class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level
    @Volatile private var finishEarly = false

    /** returns the recorded file, or null when nothing was said */
    suspend fun record(): File? {
        stop()
        finishEarly = false
        val file = File(context.cacheDir, "talk.m4a").apply { delete() }
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(16_000)
        r.setAudioEncodingBitRate(32_000)
        r.setAudioChannels(1)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        var heard = false
        var quietMs = 0L
        var total = 0L
        var noise = 600f
        while (total < 60_000 && !finishEarly) {
            delay(100)
            total += 100
            val amp = runCatching { r.maxAmplitude.toFloat() }.getOrDefault(0f)
            if (total < 400) noise = maxOf(noise, amp * 1.4f)
            _level.value = (amp / 9_000f).coerceIn(0f, 1f)
            if (amp > noise * 2.2f && amp > 1_500f) { heard = true; quietMs = 0 } else quietMs += 100
            if (heard && quietMs >= 1_400) break
            if (!heard && total >= 9_000) break
        }
        runCatching { r.stop() }
        r.release()
        recorder = null
        _level.value = 0f
        return if (heard || finishEarly) file.takeIf { it.length() > 2_000 } else null
    }

    fun finish() { finishEarly = true }

    fun stop() {
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
        _level.value = 0f
    }
}
