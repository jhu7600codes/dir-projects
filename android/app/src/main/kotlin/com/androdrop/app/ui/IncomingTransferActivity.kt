package com.androdrop.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androdrop.app.data.ApiClient
import com.androdrop.app.data.DeviceStore
import com.androdrop.app.data.IncomingTransferSummary
import com.androdrop.app.data.RespondRequest
import com.androdrop.app.data.TransferFileSummary
import com.androdrop.app.data.TransferFileView
import com.androdrop.app.ui.theme.AndrodropTheme
import com.androdrop.app.util.formatBytes
import kotlinx.coroutines.launch

private sealed interface IncomingUiState {
    object Loading : IncomingUiState
    data class Pending(val deviceId: String, val transfer: IncomingTransferSummary, val busy: Boolean = false) :
        IncomingUiState
    data class Accepted(val files: List<TransferFileView>) : IncomingUiState
    object Declined : IncomingUiState
    data class Error(val message: String) : IncomingUiState
}

class IncomingTransferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transferId = intent.getStringExtra("transfer_id")

        setContent {
            AndrodropTheme {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (transferId == null) {
                            Text("No transfer specified", color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            IncomingTransferRoute(transferId, onDone = { finish() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingTransferRoute(transferId: String, onDone: () -> Unit) {
    var state by remember { mutableStateOf<IncomingUiState>(IncomingUiState.Loading) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(transferId) {
        val device = DeviceStore(context.applicationContext).current()
        if (device == null) {
            state = IncomingUiState.Error("This device isn't paired yet")
            return@LaunchedEffect
        }
        try {
            val detail = ApiClient.service.getTransfer(transferId, device.deviceId)
            state = IncomingUiState.Pending(
                deviceId = device.deviceId,
                transfer = IncomingTransferSummary(
                    id = detail.id,
                    senderName = detail.senderName,
                    files = detail.files.map { TransferFileSummary(it.name, it.size, it.type) },
                    createdAt = detail.createdAt,
                ),
            )
        } catch (e: Exception) {
            state = IncomingUiState.Error(e.message ?: "Couldn't load this transfer")
        }
    }

    when (val s = state) {
        is IncomingUiState.Loading -> CircularProgressIndicator()
        is IncomingUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
        is IncomingUiState.Declined -> Text("Declined", color = MaterialTheme.colorScheme.onSurface)
        is IncomingUiState.Pending -> IncomingTransferCard(
            transfer = s.transfer,
            busy = s.busy,
            onAccept = {
                state = s.copy(busy = true)
                scope.launch {
                    try {
                        val result = ApiClient.service.respond(
                            transferId,
                            RespondRequest(s.deviceId, "accept"),
                        )
                        state = IncomingUiState.Accepted(result.files ?: emptyList())
                    } catch (e: Exception) {
                        state = IncomingUiState.Error(e.message ?: "Couldn't accept this transfer")
                    }
                }
            },
            onDecline = {
                state = s.copy(busy = true)
                scope.launch {
                    try {
                        ApiClient.service.respond(transferId, RespondRequest(s.deviceId, "decline"))
                        state = IncomingUiState.Declined
                    } catch (e: Exception) {
                        state = IncomingUiState.Error(e.message ?: "Couldn't decline this transfer")
                    }
                }
            },
        )
        is IncomingUiState.Accepted -> AcceptedFilesList(s.files, onDone)
    }
}

@Composable
private fun AcceptedFilesList(files: List<TransferFileView>, onDone: () -> Unit) {
    val context = LocalContext.current
    Column {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) { Text("✅") }
        }
        Text(
            "Accepted",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
        )
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(files) { file ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(file.name)
                        Text(formatBytes(file.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(
            onClick = {
                files.forEach { file ->
                    if (file.url != null) {
                        val request = DownloadManager.Request(Uri.parse(file.url))
                            .setTitle(file.name)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.name)
                        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    }
                }
                onDone()
            },
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp),
        ) {
            Text("Download all")
        }
    }
}
