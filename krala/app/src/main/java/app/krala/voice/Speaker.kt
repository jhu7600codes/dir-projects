package app.krala.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import app.krala.core.VoiceScript
import app.krala.core.VoiceScript.Segment
import app.krala.core.VoiceScript.Tone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performs [VoiceScript] segments with the system text to speech: pitch, speed and volume per
 * segment, real silences for beats, a laugh for [laughs]. Segments are queued as they arrive so
 * krala starts talking while the reply is still streaming.
 */
@Singleton
class Speaker @Inject constructor(@ApplicationContext private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = CompletableDeferred<Boolean>()
    private val counter = AtomicInteger()
    private val pending = mutableSetOf<String>()
    private var lastQueued: String? = null
    private var russian = false

    /** true while anything is being spoken or queued */
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    /** the tone being spoken right now, for the star's energy */
    private val _tone = MutableStateFlow(Tone.NEUTRAL)
    val tone: StateFlow<Tone> = _tone

    private val tones = mutableMapOf<String, Tone>()

    suspend fun init(): Boolean {
        if (tts != null) return ready.await()
        ready = CompletableDeferred()
        tts = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {
                    tones[id]?.let { _tone.value = it }
                }

                override fun onDone(id: String) = finished(id)

                @Deprecated("old api")
                override fun onError(id: String) = finished(id)
                override fun onError(id: String, errorCode: Int) = finished(id)
                override fun onStop(id: String, interrupted: Boolean) = finished(id)
            })
        }
        return ready.await()
    }

    private fun finished(id: String) {
        synchronized(pending) {
            pending.remove(id)
            tones.remove(id)
            if (pending.isEmpty()) _speaking.value = false
        }
    }

    /** picks the language from the reply and a girl's voice when the engine has one */
    fun setLanguageFor(text: String, appLanguage: String) {
        val t = tts ?: return
        russian = VoiceScript.isCyrillic(text) || (text.isBlank() && appLanguage == "ru")
        val locale = if (russian) Locale("ru", "RU") else Locale.US
        if (t.voice?.locale?.language == locale.language) return
        runCatching { t.language = locale }
        runCatching {
            val voices = t.voices?.filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }.orEmpty()
            pickVoice(voices)?.let { t.voice = it }
        }
    }

    private fun pickVoice(voices: List<Voice>): Voice? {
        if (voices.isEmpty()) return null
        val female = voices.filter { v ->
            val n = v.name.lowercase()
            "female" in n || v.features.any { it.contains("female", true) } ||
                // google's local voices: these letter codes are the female ones
                listOf("-sfg-", "-tpc-", "-tpf-", "-iol-", "-iob-", "-dfc-", "-rug-", "-ruf-", "-ruc-").any { it in n }
        }
        return (female.ifEmpty { voices }).maxByOrNull { it.quality }
    }

    fun speak(segments: List<Segment>) {
        val t = tts ?: return
        for (s in segments) {
            val id = "k" + counter.incrementAndGet()
            synchronized(pending) { pending += id; _speaking.value = true }
            when (s) {
                is Segment.Pause -> t.playSilentUtterance(s.ms, TextToSpeech.QUEUE_ADD, id)
                is Segment.Laugh -> {
                    tones[id] = Tone.EXCITED
                    t.setPitch(1.35f); t.setSpeechRate(1.3f)
                    t.speak(VoiceScript.laughText(s.kind, russian), TextToSpeech.QUEUE_ADD, params(1f), id)
                }
                is Segment.Speak -> {
                    tones[id] = s.tone
                    t.setPitch(s.tone.pitch); t.setSpeechRate(s.tone.rate)
                    t.speak(s.text, TextToSpeech.QUEUE_ADD, params(s.tone.volume), id)
                }
            }
            lastQueued = id
        }
        t.setPitch(1f); t.setSpeechRate(1f)
    }

    private fun params(volume: Float) = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }

    fun stop() {
        tts?.stop()
        synchronized(pending) { pending.clear(); tones.clear(); _speaking.value = false }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
