package dev.jhu.crossi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jhu.crossi.ui.nav.CrossiNavHost
import dev.jhu.crossi.ui.theme.CrossiTheme
import kotlinx.coroutines.launch

// AppCompatActivity for per-app language switching (appcompat stores the choice itself).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepo = (application as CrossiApp).container.settings
        val settingsFlow = settingsRepo.settings
        setContent {
            val scope = rememberCoroutineScope()
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // wait for settings so the theme doesn't flash
            settings?.let { current ->
                CrossiTheme(themeMode = current.themeMode, dynamicColor = current.dynamicColor) {
                    CrossiNavHost(showHowToPlay = !current.seenHowToPlay)
                    // after an update (or first install) with Easy Mode on, ask once if it should stay on
                    if (current.easyMode && current.easyModeAskedVersion < BuildConfig.VERSION_CODE) {
                        EasyModeQuestion { keepOn ->
                            scope.launch { settingsRepo.answerEasyModeQuestion(keepOn, BuildConfig.VERSION_CODE) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EasyModeQuestion(onAnswer: (keepOn: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.easy_mode_warning_title)) },
        text = { Text(stringResource(R.string.easy_mode_warning_text)) },
        confirmButton = { Button(onClick = { onAnswer(true) }) { Text(stringResource(R.string.easy_mode_keep)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.easy_mode_turn_off)) } },
    )
}
