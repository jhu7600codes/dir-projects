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
class Speaker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: app.krala.data.settings.SettingsRepository,
) {
    data class EngineInfo(val packageName: String, val label: String)
    data class VoiceInfo(val name: String, val language: String, val installed: Boolean, val network: Boolean, val quality: Int)

    private var engine: String? = null
    private var prefEn: String? = null
    private var prefRu: String? = null
    private var rate = 1f
    private var pitch = 1f
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

    /** set when speech can't work (no engine, no voice data, engine errors) */
    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem

    /** counts utterances that actually started making sound */
    private val _started = MutableStateFlow(0)
    val started: StateFlow<Int> = _started

    suspend fun init(): Boolean {
        val s = settings.current()
        prefEn = s.ttsVoiceEn; prefRu = s.ttsVoiceRu; rate = s.voiceRate; pitch = s.voicePitch
        if (tts != null && engine != s.ttsEngine) shutdown()
        if (tts != null) return ready.await()
        engine = s.ttsEngine
        ready = CompletableDeferred()
        val listener = TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.SUCCESS) _problem.value = "no text to speech engine works on this phone"
            ready.complete(status == TextToSpeech.SUCCESS)
        }
        tts = (if (engine != null) TextToSpeech(context, listener, engine) else TextToSpeech(context, listener)).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {
                    tones[id]?.let { _tone.value = it }
                    _started.value++
                }

                override fun onDone(id: String) = finished(id)

                @Deprecated("old api")
                override fun onError(id: String) = finished(id)
                override fun onError(id: String, errorCode: Int) {
                    _problem.value = when (errorCode) {
                        TextToSpeech.ERROR_NOT_INSTALLED_YET -> "the voice is still downloading, try again in a minute"
                        TextToSpeech.ERROR_NETWORK, TextToSpeech.ERROR_NETWORK_TIMEOUT -> "the voice needs internet"
                        else -> "text to speech failed (error $errorCode)"
                    }
                    finished(id)
                }
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
        _problem.value = null
        if (t.voice?.locale?.language == locale.language && installed(t.voice)) return
        var result = runCatching { t.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (result < 0) {
            // no data for that language: the phone's default voice is better than silence
            result = runCatching { t.setLanguage(Locale.getDefault()) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (result < 0) result = runCatching { t.setLanguage(Locale.US) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (result < 0) _problem.value = "no voice installed for ${locale.displayLanguage}. install voice data in settings → system → languages → text to speech"
            return
        }
        val wanted = if (russian) prefRu else prefEn
        if (wanted != null) {
            t.voices?.firstOrNull { it.name == wanted && installed(it) }?.let { t.voice = it; return }
        }
        runCatching {
            // only voices that are really on the phone: listed but not downloaded ones play nothing
            val voices = t.voices?.filter { it.locale.language == locale.language && installed(it) }.orEmpty()
            val local = voices.filter { !it.isNetworkConnectionRequired }
            pickVoice(local.ifEmpty { voices })?.let { t.voice = it }
        }
    }

    private fun installed(v: Voice?): Boolean =
        v != null && v.features?.any { it.equals("notInstalled", true) } != true

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
                    t.setPitch(1.35f * pitch); t.setSpeechRate(1.3f * rate)
                    t.speak(VoiceScript.laughText(s.kind, russian), TextToSpeech.QUEUE_ADD, params(1f), id)
                }
                is Segment.Speak -> {
                    tones[id] = s.tone
                    t.setPitch(s.tone.pitch * pitch); t.setSpeechRate(s.tone.rate * rate)
                    t.speak(s.text, TextToSpeech.QUEUE_ADD, params(s.tone.volume), id)
                }
            }
            lastQueued = id
        }
        t.setPitch(1f); t.setSpeechRate(1f)
    }

    private fun params(volume: Float) = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }

    // ---- for the voice settings screen ----

    suspend fun engines(): List<EngineInfo> {
        init()
        return tts?.engines?.map { EngineInfo(it.name, it.label) }.orEmpty()
    }

    fun currentEngine(): String? = runCatching { tts?.defaultEngine }.getOrNull()?.let { engine ?: it }

    suspend fun voices(): List<VoiceInfo> {
        init()
        return tts?.voices.orEmpty()
            .filter { it.locale.language in setOf("en", "ru") }
            .map { VoiceInfo(it.name, it.locale.language, installed(it), it.isNetworkConnectionRequired, it.quality) }
            .sortedWith(compareBy<VoiceInfo> { it.language }.thenBy { !it.installed }.thenBy { it.network }.thenByDescending { it.quality }.thenBy { it.name })
    }

    /** says a line with one voice (or the default for that language when null) */
    suspend fun preview(voiceName: String?, russian: Boolean) {
        init()
        val t = tts ?: return
        stop()
        val v = voiceName?.let { n -> t.voices?.firstOrNull { it.name == n } }
        if (v != null) t.voice = v else t.language = if (russian) Locale("ru", "RU") else Locale.US
        t.setPitch(pitch); t.setSpeechRate(rate)
        val id = "preview" + counter.incrementAndGet()
        synchronized(pending) { pending += id; _speaking.value = true }
        t.speak(if (russian) "привет, это крала. я уже украла твой последний кусок пиццы." else "hey, it's krala. i already stole your last slice.", TextToSpeech.QUEUE_FLUSH, params(1f), id)
    }

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
