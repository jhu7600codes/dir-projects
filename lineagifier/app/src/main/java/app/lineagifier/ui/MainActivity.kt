package app.lineagifier.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val binderReceived = Shizuku.OnBinderReceivedListener { runOnUiThread { vm.refreshShizuku() } }
    private val binderDead = Shizuku.OnBinderDeadListener { runOnUiThread { vm.refreshShizuku() } }
    private val permResult = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { vm.refreshShizuku() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permResult)

        setContent {
            LineageTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val s by vm.state.collectAsStateWithLifecycle()
                    BackHandler(enabled = s.screen != Screen.START && !s.running) {
                        vm.go(
                            when (s.screen) {
                                Screen.TWEAKS -> Screen.PICK
                                else -> Screen.START
                            }
                        )
                    }
                    when (s.screen) {
                        Screen.START -> StartScreen(s, vm, onRequestPermission = { requestShizuku() })
                        Screen.PICK -> PickScreen(s, vm)
                        Screen.TWEAKS -> TweaksScreen(s, vm)
                        Screen.APPLY -> ApplyScreen(s, vm)
                        Screen.MANAGE -> ManageScreen(s, vm)
                    }
                }
            }
        }
    }

    private fun requestShizuku() {
        try { Shizuku.requestPermission(1) } catch (t: Throwable) { vm.refreshShizuku() }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshShizuku()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permResult)
        super.onDestroy()
    }
}
