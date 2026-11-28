package app.lineagifier.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getStringExtra("screen")?.let { n -> Screen.entries.firstOrNull { it.name == n }?.let { vm.go(it) } }
        setContent {
            LineageTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val s by vm.state.collectAsStateWithLifecycle()
                    BackHandler(enabled = s.screen != Screen.HOME && !s.running) {
                        vm.go(if (s.screen == Screen.TWEAKS && s.picks.isNotEmpty()) Screen.PICK else Screen.HOME)
                    }
                    when (s.screen) {
                        Screen.HOME -> HomeScreen(s, vm)
                        Screen.PICK -> PickScreen(s, vm)
                        Screen.TWEAKS -> TweaksScreen(s, vm)
                        Screen.APPLY -> ApplyScreen(s, vm)
                        Screen.MANAGE -> ManageScreen(s, vm)
                        Screen.MICROG -> MicrogScreen(s, vm)
                        Screen.CONTROL -> ControlScreen(s, vm)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("screen")?.let { n -> Screen.entries.firstOrNull { it.name == n }?.let { vm.go(it) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshRoot()
    }
}
