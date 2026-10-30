package app.venderian

import android.app.Application
import android.content.Intent
import android.speech.tts.TextToSpeech
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
    private var spanish = false

    /** whether the phone has a spanish voice (null until text to speech has started) */
    var spanishVoice by mutableStateOf<Boolean?>(null)
        private set
    private var pending: (() -> Unit)? = null

    /** the phone's text to speech, started the first time something is said */
    private fun withTts(say: () -> Unit) {
        if (ttsReady) return say()
        pending = say
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                checkSpanish()
                tts?.setSpeechRate(0.85f)
                pending?.invoke()
                pending = null
            }
        }
    }

    private fun checkSpanish() {
        // a voice that's listed but not downloaded yet reports LANG_MISSING_DATA
        spanish = (tts?.isLanguageAvailable(SPANISH) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE
        spanishVoice = spanish
    }

    /** looks again (after coming back from the voice download screen) */
    fun refreshVoices() = withTts { checkSpanish() }

    /**
     * opens the phone's voice engine on its "install voice data" screen, where the spanish
     * voice is one tap. apps can't download voices themselves. falls back to the text to
     * speech settings if the engine has no such screen.
     */
    fun spanishVoiceIntent(): Intent {
        val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tts?.defaultEngine?.let { install.setPackage(it) }
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
        if (spanish) {
            tts?.language = SPANISH
            tts?.speak(Speech.spanish(roman), TextToSpeech.QUEUE_FLUSH, null, "venderian")
        } else {
            tts?.language = Locale.US
            tts?.speak(Speech.english(roman), TextToSpeech.QUEUE_FLUSH, null, "venderian")
        }
    }

    companion object {
        private val SPANISH = Locale("es", "ES")
    }
}
