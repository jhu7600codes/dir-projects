package app.venderian

import android.app.Application
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import app.venderian.convert.CmuDict
import app.venderian.convert.Converter
import app.venderian.convert.Lexicon
import app.venderian.convert.Speech
import app.venderian.convert.WordLookup
import app.venderian.course.Course
import app.venderian.course.SoundLessons
import app.venderian.data.Alphabet
import app.venderian.data.Holiday
import app.venderian.data.Repository
import app.venderian.data.VenderianDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class VenderianApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val lexicon by lazy { Lexicon.parse(assets.open("dictionary.json").bufferedReader().use { it.readText() }) }
    val alphabet by lazy { Alphabet.parse(assets.open("alphabet.json").bufferedReader().use { it.readText() }) }
    val lookup by lazy { WordLookup(lexicon.entries) }
    val soundLessons by lazy { SoundLessons.parse(assets.open("sounds.json").bufferedReader().use { it.readText() }) }
    val course by lazy { Course.parse(assets.open("course.json").bufferedReader().use { it.readText() }) }
    val repository by lazy { Repository(VenderianDb.create(this), alphabet, lexicon.entries, course) }

    /** cmudict is 3 mb unpacked, so it loads in the background from app start */
    val converter = scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
        Converter(CmuDict.fromGzip(assets.open("cmudict.dat")), lexicon)
    }

    override fun onCreate() {
        super.onCreate()
        converter.start()
        Holiday.enabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("holidays", true)
        // the widget follows your streak
        scope.launch { repository.progress.collect { WordWidget.refreshAll(this@VenderianApp) } }
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val pending = mutableListOf<() -> Unit>()
    private val ttsPrefs by lazy { getSharedPreferences("voice", MODE_PRIVATE) }

    /** the spanish voice we read venderian with: any country (spain, us, mexico...), if there's one */
    private var spanishLocale: Locale? = null
    private var spanishVoiceObj: Voice? = null

    /** whether the engine has a spanish voice (null until text to speech has started) */
    var spanishVoice by mutableStateOf<Boolean?>(null)
        private set

    /** which spanish was found, like "es-US", for settings */
    var spanishName by mutableStateOf<String?>(null)
        private set

    /** the speech engines on the phone (google, samsung, huawei, xiaomi...) */
    var engines by mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList())
        private set

    /** the engine picked in settings (a package name), or null for the phone's default */
    var engine by mutableStateOf<String?>(null)
        private set

    /** the engine actually in use */
    var engineInUse by mutableStateOf<String?>(null)
        private set

    /** the phone's text to speech, started the first time something is said */
    private fun withTts(say: () -> Unit) {
        if (ttsReady) return say()
        pending += say
        if (tts == null) startTts()
    }

    private fun startTts() {
        ttsReady = false
        engine = ttsPrefs.getString("engine", null)
        val listener = TextToSpeech.OnInitListener { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            val t = tts
            if (ttsReady && t != null) {
                engines = t.engines
                engineInUse = engine ?: t.defaultEngine
                checkSpanish(t)
                t.setSpeechRate(0.85f)
                pending.toList().forEach { it() }
            } else {
                spanishVoice = false
            }
            pending.clear()
        }
        tts = engine?.let { TextToSpeech(this, listener, it) } ?: TextToSpeech(this, listener)
    }

    /** starts the engine again: a voice downloaded meanwhile only shows up after a restart */
    private fun restartTts() {
        tts?.shutdown()
        tts = null
        startTts()
    }

    private fun checkSpanish(t: TextToSpeech) {
        // installed spanish voices, from any country. a listed but not downloaded voice says notInstalled
        val voices = runCatching { t.voices }.getOrNull().orEmpty().filter {
            it.locale.language == "es" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }
        // spain first (closest to venderian), offline voices before network ones
        val voice = voices.sortedWith(compareBy({ it.locale.country != "ES" }, { it.isNetworkConnectionRequired })).firstOrNull()
        // engines that don't list voices still answer isLanguageAvailable
        val locale = voice?.locale ?: SPANISH_LOCALES.firstOrNull {
            runCatching { t.isLanguageAvailable(it) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE
        }
        spanishVoiceObj = voice
        spanishLocale = locale
        spanishVoice = locale != null
        spanishName = locale?.toLanguageTag()
    }

    /** looks again for a spanish voice (after coming back from the voice download screen) */
    fun refreshVoices() {
        if (spanishVoice == true) return
        if (tts == null) startTts() else restartTts()
    }

    /** settings: use this engine (null for the phone's default) */
    fun useEngine(pkg: String?) {
        ttsPrefs.edit().putString("engine", pkg).apply()
        restartTts()
    }

    /**
     * opens the voice engine on its "install voice data" screen, where the spanish voice is
     * one tap. apps can't download voices themselves. falls back to the text to speech
     * settings if the engine has no such screen.
     */
    fun spanishVoiceIntent(): Intent {
        val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        engineInUse?.let { install.setPackage(it) }
        return if (install.resolveActivity(packageManager) != null) install
        else Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** speaks an english sound hint (letter lessons) */
    fun speak(text: String) = withTts {
        tts?.language = Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hint")
    }

    /** says venderian (romanized) out loud, with a spanish voice if the phone has one */
    fun speakVenderian(roman: String) = withTts {
        val locale = spanishLocale
        if (locale != null) {
            val voice = spanishVoiceObj
            if (voice != null) tts?.voice = voice else tts?.language = locale
            tts?.speak(Speech.spanish(roman), TextToSpeech.QUEUE_FLUSH, null, "venderian")
        } else {
            tts?.language = Locale.US
            tts?.speak(Speech.english(roman), TextToSpeech.QUEUE_FLUSH, null, "venderian")
        }
    }

    companion object {
        private val SPANISH_LOCALES = listOf(Locale("es", "ES"), Locale("es", "US"), Locale("es", "MX"), Locale("es"))
    }
}
