package com.maytube.app.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.maytube.app.data.ServerConfig

/**
 * Keeps navigation inside the configured yt2009 instance, hands truly
 * external links (real youtube.com support pages, the github sponsor link
 * in the footer, etc.) off to the system, and re-applies the mobile
 * CSS/JS injection after every page load.
 */
class MaytubeWebViewClient(
    private val context: Context,
    private var config: ServerConfig,
    private val onPageFinishedListener: (String?) -> Unit
) : WebViewClient() {

    fun updateConfig(newConfig: ServerConfig) {
        config = newConfig
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val scheme = uri.scheme?.lowercase()

        if (scheme != "http" && scheme != "https") {
            // mailto:, intent:, market:, etc -- let the system handle it
            return openExternally(uri)
        }

        val host = uri.host
        if (host != null && !isSameInstance(host, uri.port)) {
            return openExternally(uri)
        }

        return false
    }

    private fun isSameInstance(host: String, port: Int): Boolean {
        val effectivePort = if (port == -1) (if (config.useHttps) 443 else 80) else port
        val configPort = config.port
        return host.equals(config.host, ignoreCase = true) && effectivePort == configPort
    }

    private fun openExternally(uri: Uri): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            true // swallow: nothing sensible to do with e.g. an unhandled custom scheme
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        view.evaluateJavascript(MobileInjector.buildInjectionScript(config), null)
        onPageFinishedListener(url)
    }

    companion object {
        fun baseUri(config: ServerConfig): Uri = config.baseUrl.toUri()
    }
}
