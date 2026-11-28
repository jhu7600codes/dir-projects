package app.krala

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import app.krala.data.chat.AttachmentStore
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import app.krala.data.sync.SupabaseHolder
import app.krala.ui.lock.LockScreen
import app.krala.ui.nav.KralaNav
import app.krala.ui.theme.KralaTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var router: IntentRouter
    @Inject lateinit var attachments: AttachmentStore
    @Inject lateinit var supabase: SupabaseHolder

    private var ready by mutableStateOf(false)
    private var locked by mutableStateOf(false)
    private var stoppedAt = 0L
    private var authenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !ready }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            val s = settings.current()
            if (savedInstanceState == null && s.biometricLock) locked = true
            ready = true
        }
        if (savedInstanceState == null) handleIntent(intent)
        // android 13+: ask once so finished replies and krala's texts can show up
        if (Build.VERSION.SDK_INT >= 33) lifecycleScope.launch {
            if (!settings.askedNotifyOnce() &&
                ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7)
        }

        setContent {
            val s by settings.settings.collectAsState(initial = AppSettings())
            LaunchedEffect(s.language) {
                val want = if (s.language.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(s.language)
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != want.toLanguageTags()) AppCompatDelegate.setApplicationLocales(want)
            }
            KralaTheme(mode = s.themeMode, dynamic = s.dynamicColor, haptics = s.haptics) {
                if (locked && s.biometricLock) LockScreen(onUnlock = ::authenticate)
                else KralaNav(router, attachments::file)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // short trips (photo picker, share sheet) don't lock; coming back later does
        if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > LOCK_GRACE_MS) {
            lifecycleScope.launch { if (settings.settings.first().biometricLock) locked = true }
        }
        stoppedAt = 0
    }

    private fun authenticate() {
        if (authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authenticating = false
                locked = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authenticating = false
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_krala))
            .apply {
                if (Build.VERSION.SDK_INT >= 30) {
                    setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
                } else {
                    setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK)
                    setNegativeButtonText(getString(R.string.cancel))
                }
            }
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { authenticating = false }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = listOfNotNull(
                    intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { s -> intent.getStringExtra(Intent.EXTRA_TEXT)?.contains(s) != true },
                    intent.getStringExtra(Intent.EXTRA_TEXT),
                ).joinToString("\n").trim()
                val image = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.takeIf { intent.type?.startsWith("image/") == true }
                if (text.isNotEmpty() || image != null) router.post(Incoming.Share(text, listOfNotNull(image)))
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
                if (text.isNotBlank()) router.post(Incoming.Share(text, emptyList()))
            }
            Intent.ACTION_VIEW -> if (intent.data?.scheme == "krala") supabase.client?.handleDeeplinks(intent)
        }
        intent.getStringExtra(EXTRA_OPEN_CHAT)?.let { router.post(Incoming.OpenChat(it)) }
    }

    companion object {
        const val EXTRA_OPEN_CHAT = "open_chat"
        private const val LOCK_GRACE_MS = 20_000L
    }
}
