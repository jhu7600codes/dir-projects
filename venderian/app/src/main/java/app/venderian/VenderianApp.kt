package app.venderian

import android.app.Application
import android.speech.tts.TextToSpeech
import app.venderian.convert.CmuDict
import app.venderian.convert.Converter
import app.venderian.convert.Lexicon
import app.venderian.convert.WordLookup
import app.venderian.course.Course
import app.venderian.course.SoundLessons
import app.venderian.data.Alphabet
import app.venderian.data.Repository
import app.venderian.data.VenderianDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        converter.start()
    }

    /** speaks a sound hint with the phone's (offline) text to speech, if it has one */
    fun speak(text: String) {
        val engine = tts
        if (engine == null) {
            tts = TextToSpeech(this) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.US
                    tts?.setSpeechRate(0.85f)
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hint")
                }
            }
        } else if (ttsReady) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hint")
        }
    }
}
