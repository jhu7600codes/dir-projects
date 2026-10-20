package app.stash.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.data.prefs.Settings
import app.stash.data.prefs.ThemeMode
import app.stash.importer.Spotify
import app.stash.providers.ProviderRegistry
import app.stash.providers.YtDlp
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settings: Settings,
    val registry: ProviderRegistry,
    private val ytDlp: YtDlp,
    private val spotify: Spotify,
) : ViewModel() {
    val ytStatus = ytDlp.status
    val updateResult = MutableStateFlow<String?>(null)

    fun updateYtDlp() = viewModelScope.launch {
        updateResult.value = "checking…"
        updateResult.value = ytDlp.update()
    }

    fun set(block: suspend Settings.() -> Unit) = viewModelScope.launch { settings.block() }
    fun logoutSpotify() = viewModelScope.launch { spotify.logout() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = hiltViewModel()
    val nav = LocalNav.current
    val actions = LocalActions.current
    val s by actions.settings.collectAsState()
    val ytStatus by vm.ytStatus.collectAsState()
    val updateResult by vm.updateResult.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val settings = s ?: return

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("settings") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
            item { SectionHeader("look") }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    val modes = ThemeMode.entries
                    modes.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = settings.themeMode == m,
                            onClick = { vm.set { setTheme(m) } },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        ) { Text(m.name.lowercase()) }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) item {
                SwitchRow("colors from wallpaper", "material you dynamic color", settings.dynamicColor) { vm.set { setDynamicColor(it) } }
            }

            item { SectionHeader("downloads") }
            item {
                SwitchRow("wi-fi only", "downloads wait for an unmetered network", settings.wifiOnlyDownloads) {
                    actions.downloads.setWifiOnly(it)
                }
            }

            item { SectionHeader("sources") }
            item {
                Text(
                    "tried in this order when something won't play or an imported song needs matching.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            items(vm.registry.all, key = { it.id }) { p ->
                val needsKey = p.id == "jamendo" && settings.jamendoClientId.isBlank()
                SwitchRow(
                    p.displayName,
                    when {
                        needsKey -> "needs a client id (below)"
                        p.id == "youtube" || p.id == "soundcloud" || p.id == "bandcamp" -> "via yt-dlp"
                        else -> "open / cc-licensed"
                    },
                    p.id !in settings.disabledProviders && !needsKey,
                    enabled = !needsKey,
                ) { on -> vm.set { setProviderEnabled(p.id, on) } }
            }
            item {
                KeyField("jamendo client id", settings.jamendoClientId, "free from devportal.jamendo.com") { vm.set { setJamendoClientId(it) } }
            }

            item { SectionHeader("yt-dlp") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("version ${settings.ytDlpVersion.ifBlank { "bundled" }}", style = MaterialTheme.typography.titleMedium)
                        Text("status: $ytStatus", style = MaterialTheme.typography.bodySmall)
                        Text("updates itself every time the app starts, because youtube keeps breaking old versions.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        updateResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                        Row(Modifier.padding(top = 8.dp)) {
                            FilledTonalButton(onClick = { vm.updateYtDlp() }) { Text("update now") }
                        }
                    }
                }
            }

            item { SectionHeader("playlist import") }
            item {
                KeyField("spotify client id", settings.spotifyClientId,
                    "from developer.spotify.com, redirect uri stash://spotify-callback") { vm.set { setSpotifyClientId(it) } }
            }
            if (settings.spotifyConnected) item {
                ListItem(
                    headlineContent = { Text("spotify connected") },
                    trailingContent = { FilledTonalButton(onClick = { vm.logoutSpotify() }) { Text("log out") } },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item {
                KeyField("apple musickit developer token (optional)", settings.appleDeveloperToken,
                    "without it, public playlist links are read from the web page", secret = true) { vm.set { setAppleDeveloperToken(it) } }
            }

            item { SectionHeader("about") }
            item {
                Text(
                    "stash · personal use only. youtube, soundcloud and bandcamp go through yt-dlp, which is against their terms, " +
                        "so keep this to yourself. jamendo, internet archive and free music archive are open sources.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun KeyField(label: String, value: String, hint: String, secret: Boolean = false, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        supportingText = { Text(hint) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        shape = MaterialTheme.shapes.medium,
        trailingIcon = if (text != value) ({
            TextButton(onClick = { onSave(text) }) { Text("save") }
        }) else null,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
