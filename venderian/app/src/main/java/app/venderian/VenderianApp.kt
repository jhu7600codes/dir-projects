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
    var engines by mutableStateOf<List<Pair<String, String>>>(emptyList())
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

    /** what happened, for settings: "starting", "ok", "failed (code)" */
    var ttsStatus by mutableStateOf("not started")
        private set

    /** every spanish voice the engine listed, with whether it's installed, for settings */
    var spanishDetails by mutableStateOf("")
        private set

    private var starting = false
    private var generation = 0
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** the speech engines installed, as (package, name), read from the system (works even if one won't start) */
    private fun installedEngines(): List<Pair<String, String>> =
        packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).map {
            it.serviceInfo.packageName to it.loadLabel(packageManager).toString()
        }.distinctBy { it.first }

    /**
     * starts the speech engine. [tryNext] is the list of engines still to try if this one
     * fails or never answers (the default first, then every other installed one).
     */
    private fun startTts(tryNext: List<String>? = null) {
        ttsReady = false
        starting = true
        engines = installedEngines()
        engine = ttsPrefs.getString("engine", null)
        val candidates = tryNext ?: (listOfNotNull(engine) + engines.map { it.first }).distinct()
        val pkg = candidates.firstOrNull()
        ttsStatus = "starting ${pkg ?: "default"}"
        // only the newest engine's answer counts: an old one can still answer after a restart,
        // and a failing engine can answer before its constructor even returns
        val gen = ++generation
        var answered = false
        val listener = TextToSpeech.OnInitListener { status ->
            main.post {
                if (gen != generation || answered) return@post
                answered = true
                onTtsReady(status, pkg, candidates.drop(1))
            }
        }
        tts = if (pkg != null) TextToSpeech(this, listener, pkg) else TextToSpeech(this, listener)
        // some engines never answer at all: give up after a while and try the next one
        main.postDelayed({
            if (gen == generation && !answered) {
                answered = true
                onTtsReady(TextToSpeech.ERROR, pkg, candidates.drop(1), timedOut = true)
            }
        }, 8_000)
    }

    private fun onTtsReady(status: Int, pkg: String?, rest: List<String>, timedOut: Boolean = false) {
        val t = tts ?: return
        if (status == TextToSpeech.SUCCESS) {
            starting = false
            ttsReady = true
            ttsStatus = "ok"
            engineInUse = pkg ?: t.defaultEngine
            checkSpanish(t)
            t.setSpeechRate(0.85f)
            pending.toList().forEach { it() }
            pending.clear()
            return
        }
        val why = if (timedOut) "no answer" else "failed ($status)"
        if (rest.isNotEmpty()) {
            // this engine won't start: try the next installed one
            t.shutdown()
            tts = null
            ttsStatus = "${pkg ?: "default"} $why, trying ${rest.first()}"
            startTts(rest)
            return
        }
        starting = false
        ttsStatus = "${pkg ?: "default"} $why"
        spanishVoice = false
        spanishDetails = "no speech engine would start"
        pending.clear()
    }

    /** starts the engine again: a voice downloaded meanwhile only shows up after a restart */
    private fun restartTts() {
        val old = tts
        tts = null
        generation++
        old?.shutdown()
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
        spanishDetails = runCatching { t.voices }.getOrNull().orEmpty()
            .filter { it.locale.language == "es" }
            .groupBy { it.locale.toLanguageTag() }
            .entries.joinToString(", ") { (tag, vs) ->
                val installed = vs.any { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
                "$tag ${if (installed) "installed" else "not downloaded"}"
            }.ifEmpty { "the engine lists no spanish voices" }
        spanishVoiceObj = voice
        spanishLocale = locale
        spanishVoice = locale != null
        spanishName = locale?.toLanguageTag()
    }

    /** looks again for a spanish voice (after coming back from the voice download screen) */
    fun refreshVoices() {
        // already found, or a start is still on its way: leave it alone
        if (spanishVoice == true || starting) return
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
