package app.lineagifier.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** hosts lineagifier keeps inside its browser; anything else opens in the real browser */
fun isApkmirror(host: String?): Boolean = host != null && (host == "apkmirror.com" || host.endsWith(".apkmirror.com") ||
    // cloudflare's challenge pages
    host == "challenges.cloudflare.com")

/**
 * a small browser just for apkmirror: opens the app's page, lets cloudflare's check and apkmirror's download
 * page run like in any browser, and catches the final apk download to check and install it.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(s: UiState, vm: AppViewModel) {
    val entry = s.browserEntry ?: run { vm.go(Screen.PICK); return }
    var web by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf("apkmirror") }
    var loading by remember { mutableStateOf(true) }
    var url by remember { mutableStateOf(entry.apkmirror ?: "") }
    // bumping this recreates the webview after its renderer died
    var generation by remember { mutableIntStateOf(0) }
    var crashed by remember { mutableStateOf(false) }
    val external = LocalUriHandler.current
    BackHandler { if (web?.canGoBack() == true) web?.goBack() else vm.go(Screen.PICK) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${entry.name} · ${entry.lineageIds.firstOrNull() ?: ""}", fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Uri.parse(url).host + Uri.parse(url).path, color = Dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { vm.go(Screen.PICK) }) { Text("done") }
        }
        Text("pick the release for android 13 (lineage 20), then a variant marked APK (not BUNDLE), then download. lineagifier takes it from there.",
            color = Dim, fontSize = 12.sp, modifier = Modifier.background(Surface1).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
        when {
            s.downloadProgress != null -> LinearProgressIndicator(progress = { s.downloadProgress }, Modifier.fillMaxWidth())
            loading || s.apkBusy -> LinearProgressIndicator(Modifier.fillMaxWidth())
            else -> Spacer(Modifier.height(4.dp))
        }
        if (crashed) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("the page crashed (android's webview renderer stopped).", color = Dim)
                Button(onClick = { crashed = false; generation++ }, modifier = Modifier.padding(top = 12.dp)) { Text("reload") }
                OutlinedButton(onClick = { try { external.openUri(url) } catch (_: Throwable) {} }, modifier = Modifier.padding(top = 8.dp)) { Text("open in your browser instead") }
                Text("if it keeps crashing: download there, then use \"pick a file\" on the lineage apps page.", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else key(generation) { AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.White),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true      // cloudflare's check and apkmirror's download button need it
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                            if (isApkmirror(req.url.host)) return false
                            // only open other sites outside on a real tap, never on redirects the page does by itself
                            if (req.hasGesture()) try { external.openUri(req.url.toString()) } catch (_: Throwable) {}
                            return true
                        }
                        override fun onPageStarted(view: WebView, u: String, favicon: Bitmap?) { loading = true; url = u }
                        override fun onPageFinished(view: WebView, u: String) { loading = false; title = view.title ?: title; url = u }
                        // without this, a crashed renderer takes the whole app down with it
                        override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                            view.destroy()
                            if (web === view) web = null
                            loading = false; crashed = true
                            return true
                        }
                    }
                    setDownloadListener { dlUrl, userAgent, disposition, mime, _ -> vm.browserDownload(dlUrl, userAgent, disposition, mime) }
                    loadUrl(if (generation > 0 && url.isNotEmpty()) url else entry.apkmirror ?: "https://www.apkmirror.com/apk/lineageos/")
                    web = this
                }
            },
        ) }
    }
    ApkDialogs(s, vm)
}
