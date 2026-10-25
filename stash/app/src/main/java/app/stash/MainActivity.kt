package app.stash

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import app.stash.cloud.Account
import app.stash.cloud.CloudBackup
import app.stash.data.prefs.ThemeMode
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
    @Inject lateinit var account: Account
    @Inject lateinit var cloud: CloudBackup

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
            // the launcher icon follows the app: dark icon while the app is dark (unless turned off)
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            settings?.let { s ->
                val dark = when (s.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> systemDark
                }
                androidx.compose.runtime.LaunchedEffect(dark && s.darkIcon) {
                    runCatching { AppIcon.apply(this@MainActivity, dark && s.darkIcon) }
                }
            }
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

    override fun onResume() {
        super.onResume()
        // back from tapping the confirm link in the email app: finish logging in
        if (account.hasPendingSignup) lifecycleScope.launch {
            if (account.completePendingSignup()) afterLogin()
        }
    }

    private suspend fun afterLogin() {
        val added = runCatching { cloud.firstSync() }.getOrDefault(0)
        app.toast(if (added > 0) "Logged in, restored $added playlists" else "Logged in, your library is backed up")
        navRequests.send(Routes.SETTINGS)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data?.scheme == "stash" && data.host == "auth-callback") {
            lifecycleScope.launch {
                runCatching { account.handleRedirect(data) }
                    .onSuccess { afterLogin() }
                    .onFailure { app.toast(it.message ?: "Couldn't log in from that link") }
            }
            return
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            openPlayer.trySend(Unit)
            return
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_RESCAN, false)) {
            navRequests.trySend(Routes.RESCAN)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_IMPORTS, false)) {
            navRequests.trySend(Routes.IMPORT_PROGRESS)
            return
        }
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            navRequests.trySend(Routes.import(text))
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
        const val EXTRA_OPEN_IMPORTS = "open_imports"
        const val EXTRA_OPEN_RESCAN = "open_rescan"
    }
}
