package com.fivepesos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fivepesos.app.ui.screens.CoinScreen
import com.fivepesos.app.ui.theme.FivePesosTheme
import com.fivepesos.app.viewmodel.CoinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FivePesosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FivePesosApp()
                }
            }
        }
    }
}

@Composable
private fun FivePesosApp(viewModel: CoinViewModel = viewModel()) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current

    fun persistAndStore(uri: Uri, store: (Uri) -> Unit) {
        // The document picker already grants a one-time read; taking a
        // persistable grant is what lets the coin still show this image
        // after the app process is killed and relaunched.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        store(uri)
    }

    val pickHeads = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persistAndStore(it, viewModel::setCustomHeads) }
    }
    val pickTails = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persistAndStore(it, viewModel::setCustomTails) }
    }

    CoinScreen(
        state = state,
        onPrimaryAction = viewModel::primaryAction,
        onOpenSettings = { viewModel.toggleSettings(true) },
        onCloseSettings = { viewModel.toggleSettings(false) },
        onSpinForeverChange = viewModel::setSpinForever,
        onSelectSkin = viewModel::selectSkin,
        onPickHeads = { pickHeads.launch(arrayOf("image/*")) },
        onPickTails = { pickTails.launch(arrayOf("image/*")) },
        onOpenGoogleImport = { viewModel.toggleGoogleImport(true) },
        onCloseGoogleImport = { viewModel.toggleGoogleImport(false) },
        onImportImage = viewModel::importImage,
    )
}
