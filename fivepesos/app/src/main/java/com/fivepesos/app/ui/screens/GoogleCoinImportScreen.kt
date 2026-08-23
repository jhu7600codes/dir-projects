package com.fivepesos.app.ui.screens

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fivepesos.app.data.ImageTarget
import com.fivepesos.app.data.downloadImageToFile
import com.fivepesos.app.ui.theme.MetroAccent
import com.fivepesos.app.ui.theme.MetroBackground
import com.fivepesos.app.ui.theme.MetroSecondaryText
import com.fivepesos.app.ui.theme.MetroSurface
import kotlinx.coroutines.launch

/**
 * A full-screen in-app browser (Google Images, defaulting to a "coin"
 * search) for grabbing a coin photo without leaving the app. Holding down
 * on any image brings up a small "use as heads / use as tails" chooser;
 * picking one downloads that image and hands it back via [onImageChosen].
 */
@Composable
fun GoogleCoinImportScreen(
    onClose: () -> Unit,
    onImageChosen: (ImageTarget, Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingImageUrl by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) webView.goBack() else onClose()
    }

    fun importImage(target: ImageTarget, imageUrl: String) {
        pendingImageUrl = null
        isImporting = true
        val fileName = "custom_${target.name.lowercase()}_${System.currentTimeMillis()}.png"
        scope.launch {
            val uri = downloadImageToFile(context, imageUrl, fileName)
            isImporting = false
            if (uri != null) {
                onImageChosen(target, uri)
                onClose()
            } else {
                Toast.makeText(context, "Couldn't load that image -- try another one", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBackground)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "‹ back",
                color = MetroAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClose).padding(vertical = 8.dp),
            )
            Text(
                text = "search google for a coin",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "hold down on a coin photo to use it as heads or tails",
                color = MetroSecondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
        }

        // Full-bleed, deliberately outside the header's horizontal inset --
        // this is a browser, it shouldn't have dead margins down its sides.
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        setOnLongClickListener {
                            val result = hitTestResult
                            val url = result.extra
                            val isImage = result.type == WebView.HitTestResult.IMAGE_TYPE ||
                                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                            if (isImage && url != null) {
                                pendingImageUrl = url
                                true
                            } else {
                                false
                            }
                        }
                        loadUrl("https://www.google.com/search?tbm=isch&q=coin")
                    }.also { webViewRef = it }
                },
            )

            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MetroAccent)
                }
            }
        }
    }

    val imageUrl = pendingImageUrl
    if (imageUrl != null) {
        MetroImageChooserDialog(
            onDismiss = { pendingImageUrl = null },
            onUseAsHeads = { importImage(ImageTarget.HEADS, imageUrl) },
            onUseAsTails = { importImage(ImageTarget.TAILS, imageUrl) },
        )
    }
}

@Composable
private fun MetroImageChooserDialog(
    onDismiss: () -> Unit,
    onUseAsHeads: () -> Unit,
    onUseAsTails: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Metro has no rounded-corner system dialog chrome -- take over the
        // full width ourselves so this stays a sharp black rectangle.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MetroSurface)
                .padding(20.dp),
        ) {
            Text(
                text = "use this image as",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            MetroDialogButton(label = "use as heads", onClick = onUseAsHeads)
            Spacer(Modifier.height(10.dp))
            MetroDialogButton(label = "use as tails", onClick = onUseAsTails)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "cancel",
                color = MetroSecondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MetroDialogButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.Black,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroAccent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
