package dev.jhu.crossi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jhu.crossi.ui.nav.CrossiNavHost
import dev.jhu.crossi.ui.theme.CrossiTheme

// AppCompatActivity for per-app language switching (appcompat stores the choice itself).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFlow = (application as CrossiApp).container.settings.settings
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // wait for settings so the theme doesn't flash
            settings?.let { current ->
                CrossiTheme(themeMode = current.themeMode, dynamicColor = current.dynamicColor) {
                    CrossiNavHost(showHowToPlay = !current.seenHowToPlay)
                }
            }
        }
    }
}
