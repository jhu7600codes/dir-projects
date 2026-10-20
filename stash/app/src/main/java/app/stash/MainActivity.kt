package app.stash

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import app.stash.data.prefs.ThemeMode
import app.stash.importer.Spotify
import app.stash.ui.AppViewModel
import app.stash.ui.Routes
import app.stash.ui.StashRoot
import app.stash.ui.theme.StashTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var spotify: Spotify

    private val app: AppViewModel by viewModels()
    private val navRequests = Channel<String>(Channel.BUFFERED)
    private val openPlayer = Channel<Unit>(Channel.CONFLATED)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by app.settings.collectAsState()
            StashTheme(
                themeMode = settings?.themeMode ?: ThemeMode.SYSTEM,
                dynamicColor = settings?.dynamicColor ?: true,
            ) {
                StashRoot(app, navRequests.receiveAsFlow(), openPlayer.receiveAsFlow())
            }
        }

        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            openPlayer.trySend(Unit)
            return
        }
        val data = intent.data
        when {
            intent.action == Intent.ACTION_VIEW && data?.scheme == "stash" && data.host == "spotify-callback" -> {
                lifecycleScope.launch {
                    runCatching { spotify.handleRedirect(data) }
                        .onSuccess { navRequests.send(Routes.IMPORT_SPOTIFY) }
                        .onFailure { Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show() }
                }
            }
            intent.action == Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                navRequests.trySend(Routes.import(text))
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}
