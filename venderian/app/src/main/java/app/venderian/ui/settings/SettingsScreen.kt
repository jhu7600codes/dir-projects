@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.venderian.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.venderian.data.Holiday
import app.venderian.ui.app

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    // look for the spanish voice again when coming back from the download screen
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow.collect { if (it == Lifecycle.State.RESUMED) app.refreshVoices() }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Section("holiday themes") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("dress up for holidays", style = MaterialTheme.typography.bodyLarge)
                        Text("colors and a venderian greeting on the day", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(Holiday.enabled, onCheckedChange = {
                        Holiday.enabled = it
                        prefs.edit().putBoolean("holidays", it).apply()
                    })
                }
                Spacer(Modifier.height(12.dp))
                Text("try one (until you close the app)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(Holiday.override == null, { Holiday.override = null }, label = { Text("today") })
                    Holiday.entries.forEach { h ->
                        FilterChip(Holiday.override == h, {
                            Holiday.override = h
                            Holiday.enabled = true
                        }, label = { Text(h.title) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Holiday.entries.forEach { h ->
                    Text("${h.title}: ${h.date}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Section("voice") {
                val engineName = app.engines.firstOrNull { it.first == app.engineInUse }?.second ?: app.engineInUse
                Text(
                    when (app.spanishVoice) {
                        true -> "found a spanish voice (${app.spanishName}) in $engineName, venderian is read with it"
                        false -> "no spanish voice in ${engineName ?: "the speech engine"} yet, so venderian is read by an english voice (it sounds worse)"
                        null -> "checking the voices…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { runCatching { context.startActivity(app.spanishVoiceIntent()) } }) {
                        Text("download spanish voice")
                    }
                    OutlinedButton(onClick = { app.speakVenderian("vela! mi vendëri thela.") }) { Text("test") }
                }
                OutlinedButton(onClick = { app.refreshVoices() }, modifier = Modifier.padding(top = 4.dp)) { Text("check again") }
                Text(
                    "details: engine ${app.engineInUse ?: "?"} · connection ${app.ttsStatus} · ${app.spanishDetails}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (app.engines.isNotEmpty()) {
                    Text("speech engine", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    Text("if your spanish voice is in another app, pick it here",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(app.engine == null, { app.useEngine(null) }, label = { Text("phone default") })
                        app.engines.forEach { e ->
                            FilterChip(app.engine == e.first, { app.useEngine(e.first) }, label = { Text(e.second) })
                        }
                    }
                }
                Text("android doesn't let apps download voices themselves: the button opens your speech engine, pick any spanish voice there (spain is closest). it works offline after.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }

            Section("keyboard") {
                val imm = remember { context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }) { Text("turn on") }
                    OutlinedButton(onClick = { imm.showInputMethodPicker() }) { Text("switch keyboard") }
                }
            }

            Section("other apps (root, lsposed)") {
                listOf(
                    "1. in lsposed, turn on the venderian module",
                    "2. tick the apps you want (telegram, whatsapp...)",
                    "3. force-stop those apps. no reboot needed",
                    "4. on the keyboard, set the output to script",
                ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text("experimental: if an app acts weird, untick it and force-stop it again.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }

            Section("about") {
                Text("venderian ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}", style = MaterialTheme.typography.bodyMedium)
                Text("${app.course.lessons.size} lessons · ${app.lexicon.entries.size} words · offline, no accounts, no ads",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
