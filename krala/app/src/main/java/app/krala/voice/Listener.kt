package app.krala.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** wraps the system speech recognizer. must be used from the main thread. */
class Listener(private val context: Context) {
    sealed interface Event {
        data class Partial(val text: String) : Event
        data class Final(val text: String) : Event
        data class Error(val code: Int) : Event
    }

    private var recognizer: SpeechRecognizer? = null

    /**
     * Every speech service on the phone, google's app first. Some phones' default service has no
     * usable language (error 12), so the caller walks this list until one works.
     */
    fun services(): List<android.content.ComponentName?> {
        val pm = context.packageManager
        val listed = pm.queryIntentServices(Intent(android.speech.RecognitionService.SERVICE_INTERFACE), 0)
            .map { android.content.ComponentName(it.serviceInfo.packageName, it.serviceInfo.name) }
        // android's own "default voice input" setting, even if the lookup above missed it
        val system = runCatching {
            android.provider.Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
                ?.let { android.content.ComponentName.unflattenFromString(it) }
        }.getOrNull()
        // google's services by name, in case package visibility hides them
        val known = listOf(
            "com.google.android.tts" to "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService",
            "com.google.android.googlequicksearchbox" to "com.google.android.voicesearch.serviceapi.GoogleRecognitionService",
        ).map { (pkg, cls) -> android.content.ComponentName(pkg, cls) }
            .filter { c -> runCatching { pm.getServiceInfo(c, 0) }.isSuccess }
        val found = (listOfNotNull(system) + listed + known).distinctBy { it.flattenToString() }
            .sortedBy {
                when (it.packageName) {
                    "com.google.android.googlequicksearchbox" -> 0
                    "com.google.android.tts" -> 1
                    "com.google.android.as" -> 2
                    else -> 3
                }
            }
        return listOf<android.content.ComponentName?>(null) + found  // null = the phone's default
    }

    private val _level = MutableStateFlow(0f)
    /** mic loudness 0..1 */
    val level: StateFlow<Float> = _level

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(language: String?, service: android.content.ComponentName? = null, onEvent: (Event) -> Unit) {
        stop()
        val r = if (service == null) SpeechRecognizer.createSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context, service)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _level.value = 0f }
            override fun onError(error: Int) {
                _level.value = 0f
                onEvent(Event.Error(error))
            }
            override fun onResults(results: Bundle?) {
                _level.value = 0f
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onEvent(if (text.isBlank()) Event.Error(SpeechRecognizer.ERROR_NO_MATCH) else Event.Final(text))
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?.let { onEvent(Event.Partial(it)) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            if (!language.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            }
        }
        r.startListening(intent)
    }

    /** ends listening; the recognizer still delivers a final result for what was heard */
    fun finish() {
        recognizer?.stopListening()
    }

    fun stop() {
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
        _level.value = 0f
    }
}
