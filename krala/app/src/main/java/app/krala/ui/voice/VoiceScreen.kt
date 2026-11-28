package app.krala.ui.voice

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.krala.R
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import app.krala.ui.components.SectionHeader
import app.krala.ui.components.SettingRow
import app.krala.ui.components.SettingsCard
import app.krala.ui.components.clickableWithHaptic
import app.krala.ui.settings.BackTopBar
import app.krala.ui.theme.LocalHaptics
import app.krala.voice.Listener
import app.krala.voice.Speaker
import app.krala.voice.Whisper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speaker: Speaker,
    private val whisper: Whisper,
    private val settings: SettingsRepository,
) : ViewModel() {
    val appSettings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val engines = MutableStateFlow<List<Speaker.EngineInfo>>(emptyList())
    val voices = MutableStateFlow<List<Speaker.VoiceInfo>>(emptyList())
    val engineInUse = MutableStateFlow<String?>(null)
    val recognizers = MutableStateFlow<List<String>>(emptyList())
    val whisperName = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(true)
    val speaking: StateFlow<Boolean> = speaker.speaking
    val problem: StateFlow<String?> = speaker.problem

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading.value = true
        engines.value = runCatching { speaker.engines() }.getOrDefault(emptyList())
        engineInUse.value = speaker.currentEngine()
        voices.value = runCatching { speaker.voices() }.getOrDefault(emptyList())
        recognizers.value = Listener(context).services().drop(1).map { c ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(c!!.packageName, 0)).toString() }
                .getOrDefault(c!!.packageName)
        }
        whisperName.value = whisper.target()?.let { (cfg, model) -> cfg.name + " · " + model }
        loading.value = false
    }

    fun setEngine(pkg: String?) = viewModelScope.launch { settings.setTtsEngine(pkg); speaker.shutdown(); refresh() }
    fun setVoice(russian: Boolean, name: String?) = viewModelScope.launch { settings.setTtsVoice(russian, name) }
    val diag = MutableStateFlow<String?>(null)

    /** plays a line and reports what the engine actually did */
    fun preview(name: String?, russian: Boolean) = viewModelScope.launch {
        diag.value = "…"
        speaker.init()
        val before = speaker.started.value
        speaker.preview(name, russian)
        kotlinx.coroutines.delay(4_000)
        val started = speaker.started.value > before
        diag.value = listOf(
            "engine: " + (speaker.currentEngine() ?: "none"),
            "init: " + when (speaker.initOk) { true -> "ok"; false -> "failed"; null -> "pending" },
            "voices: " + voices.value.count { it.installed } + " installed / " + voices.value.size,
            "sound started: " + if (started) "yes" else "no",
            speaker.lastError?.let { "error code: $it" },
        ).filterNotNull().joinToString(" · ")
    }
    fun stopPreview() = speaker.stop()
    fun setTuning(rate: Float, pitch: Float) = viewModelScope.launch { settings.setVoiceTuning(rate, pitch); speaker.init() }
    fun setStt(mode: String) = viewModelScope.launch { settings.setSttMode(mode) }

    fun engineIntent(action: String): Intent = Intent(action).apply { engineInUse.value?.let { setPackage(it) } }
}

private fun Context.tryStart(vararg intents: Intent): Boolean {
    for (i in intents) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true } catch (_: ActivityNotFoundException) {} catch (_: SecurityException) {}
    }
    return false
}

@Composable
fun VoiceScreen(vm: VoiceViewModel, onBack: () -> Unit, onProviders: () -> Unit) {
    val context = LocalContext.current
    val h = LocalHaptics.current
    val cs = MaterialTheme.colorScheme
    val s by vm.appSettings.collectAsStateWithLifecycle()
    val engines by vm.engines.collectAsStateWithLifecycle()
    val voices by vm.voices.collectAsStateWithLifecycle()
    val engineInUse by vm.engineInUse.collectAsStateWithLifecycle()
    val recognizers by vm.recognizers.collectAsStateWithLifecycle()
    val whisperName by vm.whisperName.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val problem by vm.problem.collectAsStateWithLifecycle()
    val diag by vm.diag.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var rate by remember(s.voiceRate) { mutableFloatStateOf(s.voiceRate) }
    var pitch by remember(s.voicePitch) { mutableFloatStateOf(s.voicePitch) }
    var showAll by remember { mutableStateOf(false) }

    // coming back from the system's voice install screen: list again
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refresh() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); vm.stopPreview() }
    }

    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.voice), onBack, scroll) {
                IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, stringResource(R.string.refresh)) }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
            diag?.let { d -> item { Text(d, color = cs.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) } }
            problem?.let { p -> item { Text(p, color = cs.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) } }

            // ---------------- speaking ----------------
            item { SectionHeader(stringResource(R.string.voice_speaking)) }
            item {
                SettingsCard {
                    SettingRow(stringResource(R.string.voice_install), stringResource(R.string.voice_install_hint), Icons.Rounded.Download, onClick = {
                        h.click()
                        if (!context.tryStart(
                                vm.engineIntent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                                Intent("com.android.settings.TTS_SETTINGS"),
                            )
                        ) context.tryStart(Intent(Settings.ACTION_SETTINGS))
                    })
                    SettingRow(stringResource(R.string.voice_system_tts), null, Icons.Rounded.RecordVoiceOver, onClick = {
                        context.tryStart(Intent("com.android.settings.TTS_SETTINGS"), Intent(Settings.ACTION_SETTINGS))
                    })
                    SettingRow(
                        stringResource(R.string.voice_get_google), stringResource(R.string.voice_get_google_hint), Icons.Rounded.Shop,
                        onClick = {
                            context.tryStart(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.tts")),
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")),
                            )
                        },
                    )
                }
            }
            if (engines.size > 1) {
                item { SectionHeader(stringResource(R.string.voice_engine)) }
                item {
                    SettingsCard {
                        engines.forEach { e ->
                            ListItem(
                                headlineContent = { Text(e.label) },
                                supportingContent = { Text(e.packageName, style = MaterialTheme.typography.labelSmall) },
                                leadingContent = { RadioButton((s.ttsEngine ?: engineInUse) == e.packageName, onClick = null) },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickableWithHaptic { vm.setEngine(e.packageName) },
                            )
                        }
                    }
                }
            } else if (engines.isEmpty() && !loading) {
                item { Text(stringResource(R.string.voice_no_engine), color = cs.error, modifier = Modifier.padding(20.dp)) }
            }

            listOf(false, true).forEach { russian ->
                val list = voices.filter { it.language == (if (russian) "ru" else "en") }
                val shown = if (showAll) list else list.filter { it.installed }
                val chosen = if (russian) s.ttsVoiceRu else s.ttsVoiceEn
                item { SectionHeader(stringResource(if (russian) R.string.voice_russian else R.string.voice_english)) }
                item {
                    SettingsCard {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.voice_auto)) },
                            supportingContent = { Text(stringResource(R.string.voice_auto_hint)) },
                            leadingContent = { RadioButton(chosen == null, onClick = null) },
                            trailingContent = { IconButton(onClick = { vm.preview(null, russian) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.voice_preview)) } },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickableWithHaptic { vm.setVoice(russian, null) },
                        )
                        if (list.isEmpty() && !loading) Text(
                            stringResource(R.string.voice_none_for_language), color = cs.error, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        shown.forEach { v ->
                            ListItem(
                                headlineContent = { Text(v.name, maxLines = 1) },
                                supportingContent = {
                                    Text(
                                        listOfNotNull(
                                            if (v.installed) stringResource(R.string.voice_installed) else stringResource(R.string.voice_not_installed),
                                            if (v.network) stringResource(R.string.voice_online) else stringResource(R.string.voice_offline),
                                            stringResource(R.string.voice_quality, v.quality),
                                        ).joinToString(" · "),
                                        color = if (v.installed) cs.onSurfaceVariant else cs.error,
                                    )
                                },
                                leadingContent = { RadioButton(chosen == v.name, onClick = null, enabled = v.installed) },
                                trailingContent = { IconButton(onClick = { vm.preview(v.name, russian) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.voice_preview)) } },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickableWithHaptic { if (v.installed) vm.setVoice(russian, v.name) },
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.voice_show_all), modifier = Modifier.weight(1f))
                    Switch(showAll, onCheckedChange = { showAll = it })
                }
            }
            item { SectionHeader(stringResource(R.string.voice_tuning)) }
            item {
                SettingsCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.voice_rate, "%.2f".format(rate)), style = MaterialTheme.typography.labelLarge)
                        Slider(rate, { rate = it }, valueRange = 0.6f..1.6f, onValueChangeFinished = { vm.setTuning(rate, pitch) })
                        Text(stringResource(R.string.voice_pitch, "%.2f".format(pitch)), style = MaterialTheme.typography.labelLarge)
                        Slider(pitch, { pitch = it }, valueRange = 0.7f..1.5f, onValueChangeFinished = { vm.setTuning(rate, pitch) })
                    }
                }
            }

            // ---------------- listening ----------------
            item { SectionHeader(stringResource(R.string.voice_listening)) }
            item {
                SettingsCard {
                    listOf("auto" to R.string.stt_auto, "phone" to R.string.stt_phone, "whisper" to R.string.stt_whisper).forEach { (id, label) ->
                        ListItem(
                            headlineContent = { Text(stringResource(label)) },
                            supportingContent = {
                                Text(
                                    when (id) {
                                        "phone" -> if (recognizers.isEmpty()) stringResource(R.string.stt_phone_none) else recognizers.joinToString(", ")
                                        "whisper" -> whisperName ?: stringResource(R.string.stt_whisper_none)
                                        else -> stringResource(R.string.stt_auto_hint)
                                    }
                                )
                            },
                            leadingContent = { RadioButton(s.sttMode == id, onClick = null) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickableWithHaptic { vm.setStt(id) },
                        )
                    }
                    SettingRow(stringResource(R.string.stt_install), stringResource(R.string.stt_install_hint), Icons.Rounded.Download, onClick = {
                        context.tryStart(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS), Intent("android.settings.SPEECH_SETTINGS"), Intent(Settings.ACTION_SETTINGS))
                    })
                    if (whisperName == null) SettingRow(stringResource(R.string.stt_add_groq), stringResource(R.string.stt_add_groq_hint), Icons.Rounded.Key, onClick = onProviders)
                }
            }
        }
    }
}
