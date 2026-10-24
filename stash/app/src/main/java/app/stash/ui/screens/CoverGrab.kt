package app.stash.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.importer.PlaylistCovers
import app.stash.providers.lenientJson
import app.stash.ui.LocalNav
import app.stash.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class CoverGrabViewModel @Inject constructor(private val covers: PlaylistCovers) : ViewModel() {
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    private fun run(block: suspend () -> PlaylistCovers.Result) {
        busy.value = true
        message.value = null
        viewModelScope.launch {
            message.value = runCatching { block() }.fold(
                { r ->
                    if (r.found == 0) "Didn't find any playlists there"
                    else "Found ${r.found} playlists, set the cover on ${r.matched} of yours"
                },
                { it.message ?: "That didn't work" },
            )
            busy.value = false
        }
    }

    fun fromProfile(link: String) = run { covers.fromProfile(link) }

    /** {"spotify:playlist:id": "name", …} from the exportify page */
    fun fromExportify(json: String) = run {
        val found = lenientJson.parseToJsonElement(json).jsonObject.mapNotNull { (uri, name) ->
            val id = uri.substringAfterLast(':').takeIf { it.length == 22 } ?: return@mapNotNull null
            id to name.jsonPrimitive.content
        }.toMap()
        covers.apply(found)
    }
}

/** the "playlist covers" card on the import screen */
@Composable
fun PlaylistCoversCard() {
    val vm: CoverGrabViewModel = hiltViewModel()
    val nav = LocalNav.current
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var profile by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Playlist covers", style = MaterialTheme.typography.titleMedium)
            Text(
                "Exportify files don't include a playlist's own cover. Get them here: Stash finds your playlists " +
                    "on Spotify and matches them to yours by name. It remembers them, so later imports get covers by themselves.",
                style = MaterialTheme.typography.bodySmall,
            )
            FilledTonalButton(onClick = { nav.navigate(Routes.COVER_GRAB) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Get all covers through Exportify")
            }
            OutlinedTextField(
                value = profile, onValueChange = { profile = it }, singleLine = true,
                label = { Text("Or your Spotify profile link") },
                placeholder = { Text("https://open.spotify.com/user/…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.fromProfile(profile) }, enabled = profile.isNotBlank() && !busy) { Text("Get covers") }
                Spacer(Modifier.width(12.dp))
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Text(
                "A profile only shows about 10 public playlists; Exportify gets all of them.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/** walks every page of exportify's playlist table and hands back each playlist link + name */
private const val COLLECT_JS = """
(async function () {
  const out = {};
  const grab = () => document.querySelectorAll('a[href^="spotify:playlist:"]').forEach(a => { out[a.getAttribute('href')] = a.textContent.trim(); });
  for (let i = 0; i < 200; i++) {
    grab();
    StashBridge.progress(Object.keys(out).length);
    const next = document.querySelector('a[aria-label="Next"]');
    if (!next || next.parentElement.classList.contains('disabled')) break;
    const before = JSON.stringify(Object.keys(out));
    next.click();
    for (let w = 0; w < 30; w++) {
      await new Promise(r => setTimeout(r, 300));
      const first = document.querySelector('a[href^="spotify:playlist:"]');
      if (first && !(first.getAttribute('href') in out)) break;
    }
  }
  grab();
  StashBridge.done(JSON.stringify(out));
})();
"""

/**
 * exportify.net inside the app: log in with spotify like you did for the csv files, then
 * "Grab covers" reads the playlist list exportify shows (every page) and gets their covers.
 * nothing is sent anywhere but to spotify's public oembed for the covers.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverGrabScreen() {
    val vm: CoverGrabViewModel = hiltViewModel()
    val nav = LocalNav.current
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var web by remember { mutableStateOf<WebView?>(null) }
    var collecting by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(0) }
    var noTable by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Covers from Exportify") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (collecting || busy) return@ExtendedFloatingActionButton
                    noTable = false
                    collecting = true
                    seen = 0
                    web?.evaluateJavascript(COLLECT_JS, null)
                },
                icon = { if (collecting || busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
                text = {
                    Text(
                        when {
                            collecting -> "Reading playlists… $seen"
                            busy -> "Getting covers…"
                            else -> "Grab covers"
                        }
                    )
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val note = when {
                message != null -> message
                noTable -> "No playlists on this page yet. Log in first, then tap Grab covers on the page with your playlist list."
                else -> "Log in with Spotify, wait for your playlist list, then tap Grab covers."
            }
            Text(note!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                // spotify: links in the table would try to leave the page
                                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest) =
                                    !request.url.scheme.orEmpty().startsWith("http")
                            }
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun progress(n: Int) { post { seen = n } }

                                @JavascriptInterface
                                fun done(json: String) {
                                    post {
                                        collecting = false
                                        if (json.length <= 2) noTable = true else vm.fromExportify(json)
                                    }
                                }
                            }, "StashBridge")
                            loadUrl("https://exportify.net")
                            web = this
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}
