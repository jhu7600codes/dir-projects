package com.androdrop.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.androdrop.app.data.ApiClient
import com.androdrop.app.data.DeviceStore
import com.androdrop.app.data.TransferDetailResponse
import com.androdrop.app.ui.theme.AndrodropTheme
import com.androdrop.app.util.formatBytes
import com.androdrop.app.util.resolveFileInfo
import com.androdrop.app.util.uriMultipartPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The Android equivalent of the web app's /send flow — reached either from
 * another app's native share sheet (no root/Xposed needed, standard
 * ACTION_SEND) or from MainActivity's "Send a file" button, which launches
 * this with ACTION_PICK_AND_SEND and no attachments yet.
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialUris: List<Uri> = when (intent?.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { listOf(it) } ?: emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }

        setContent {
            AndrodropTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    SendRoute(initialUris = initialUris, onDone = { finish() })
                }
            }
        }
    }

    companion object {
        const val ACTION_PICK_AND_SEND = "com.androdrop.app.action.PICK_AND_SEND"
    }
}

private sealed interface SendUiState {
    object Sending : SendUiState
    data class Status(val detail: TransferDetailResponse) : SendUiState
    data class Error(val message: String) : SendUiState
}

@Composable
private fun SendRoute(initialUris: List<Uri>, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uris by remember { mutableStateOf(initialUris) }
    var code by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf<String?>(null) }
    var targetDeviceId by remember { mutableStateOf<String?>(null) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var sendState by remember { mutableStateOf<SendUiState?>(null) }

    val pickFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { picked -> if (picked.isNotEmpty()) uris = picked }

    LaunchedEffect(Unit) {
        if (initialUris.isEmpty()) pickFiles.launch("*/*")
    }

    LaunchedEffect(code) {
        targetName = null
        targetDeviceId = null
        resolveError = null
        val normalized = code.trim().uppercase()
        if (normalized.length != 6) return@LaunchedEffect
        delay(400)
        resolving = true
        try {
            val device = DeviceStore(context.applicationContext).current()
            val result = ApiClient.service.resolveCode(normalized)
            if (device != null && result.deviceId == device.deviceId) {
                resolveError = "That's this device — pair from a different one"
            } else {
                targetName = result.name
                targetDeviceId = result.deviceId
            }
        } catch (e: Exception) {
            resolveError = "Device not found"
        } finally {
            resolving = false
        }
    }

    val state = sendState
    if (state != null) {
        SendStatusScreen(state, onDone)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Send a file", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("Recipient's pairing code", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it.uppercase() },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            resolving -> Text("Looking up device…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            targetName != null -> Text("Sending to $targetName", color = MaterialTheme.colorScheme.primary)
            resolveError != null -> Text(resolveError ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Files", style = MaterialTheme.typography.labelLarge)
        LazyColumn(modifier = Modifier.padding(top = 8.dp).weight(1f, fill = false)) {
            items(uris) { uri ->
                val info = remember(uri) { resolveFileInfo(context, uri) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(info.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (info.size >= 0) {
                            Text(formatBytes(info.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = targetDeviceId != null && uris.isNotEmpty(),
            onClick = {
                sendState = SendUiState.Sending
                scope.launch {
                    try {
                        val device = DeviceStore(context.applicationContext).pair()
                        val parts: List<MultipartBody.Part> = uris.map { uriMultipartPart(context, it) }
                        val created = ApiClient.service.createTransfer(
                            senderDeviceId = device.deviceId.toRequestBody("text/plain".toMediaTypeOrNull()),
                            targetDeviceId = targetDeviceId!!.toRequestBody("text/plain".toMediaTypeOrNull()),
                            files = parts,
                        )
                        val detail = ApiClient.service.getTransfer(created.transferId)
                        sendState = SendUiState.Status(detail)
                    } catch (e: Exception) {
                        sendState = SendUiState.Error(e.message ?: "Send failed")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Send")
        }
    }
}

@Composable
private fun SendStatusScreen(state: SendUiState, onDone: () -> Unit) {
    var current by remember { mutableStateOf(state) }

    LaunchedEffect(state) {
        val transferId = (state as? SendUiState.Status)?.detail?.id ?: return@LaunchedEffect
        while (true) {
            delay(4000)
            try {
                val detail = ApiClient.service.getTransfer(transferId)
                current = SendUiState.Status(detail)
                if (detail.status != "pending") break
            } catch (_: Exception) {
                // keep polling
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        when (val s = current) {
            is SendUiState.Sending -> {
                CircularProgressIndicator()
                Text("Sending…", modifier = Modifier.padding(top = 16.dp))
            }
            is SendUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            is SendUiState.Status -> {
                val (emoji, title) = when (s.detail.status) {
                    "accepted" -> "✅" to "Accepted"
                    "declined" -> "🚫" to "Declined"
                    "expired" -> "⌛" to "Expired"
                    else -> "📤" to "Waiting for response…"
                }
                Text(emoji, style = MaterialTheme.typography.displayMedium)
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                Text(
                    "To ${s.detail.targetName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Done")
        }
    }
}
