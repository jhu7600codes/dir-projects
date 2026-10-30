package app.venderian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import app.venderian.VenderianApp

@Composable
fun app(): VenderianApp = LocalContext.current.applicationContext as VenderianApp

/** a viewModel built from the app container */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (VenderianApp) -> VM): VM {
    val app = app()
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = create(app) as T
    })
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onOpenDictionary: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (onOpenDictionary != null) {
                FilledTonalIconButton(onClick = onOpenDictionary) { Icon(Icons.AutoMirrored.Outlined.MenuBook, "dictionary") }
            }
            if (onOpenSettings != null) {
                FilledTonalIconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "settings") }
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** says venderian out loud (see VenderianApp.speakVenderian) */
@Composable
fun SpeakButton(roman: String, modifier: Modifier = Modifier) {
    val app = app()
    FilledTonalIconButton(onClick = { app.speakVenderian(roman) }, modifier = modifier) {
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, "say it")
    }
}

/** on holidays: the greeting, in script, with its translation */
@Composable
fun HolidayBanner() {
    val h = app.venderian.data.Holiday.today() ?: return
    androidx.compose.material3.Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                VText(app.venderian.convert.Sounds.scriptOfText(h.greeting), size = 26.sp)
                Text(h.greeting, style = MaterialTheme.typography.titleMedium)
                Text(h.english, style = MaterialTheme.typography.bodySmall)
            }
            SpeakButton(h.greeting)
        }
    }
}

/**
 * when the phone has no spanish voice: an offer to download it (venderian sounds a lot
 * better read by one). goes away once it's there, or when dismissed.
 */
@Composable
fun SpanishVoiceCard() {
    val app = app()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("voice", android.content.Context.MODE_PRIVATE) }
    var dismissed by remember { mutableStateOf(prefs.getBoolean("dismissed", false)) }
    // start text to speech so we know, and look again whenever the app comes back to the front
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow.collect { if (it == androidx.lifecycle.Lifecycle.State.RESUMED) app.refreshVoices() }
    }
    if (dismissed || app.spanishVoice != false) return
    androidx.compose.material3.Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("get the spanish voice", style = MaterialTheme.typography.titleMedium)
            Text(
                "venderian sounds much better read by a spanish voice: same vowels, same r. it's free and works offline.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                androidx.compose.material3.TextButton(onClick = {
                    prefs.edit().putBoolean("dismissed", true).apply()
                    dismissed = true
                }) { Text("not now") }
                androidx.compose.material3.FilledTonalButton(onClick = {
                    runCatching { context.startActivity(app.spanishVoiceIntent()) }
                }) { Text("download") }
            }
        }
    }
}
