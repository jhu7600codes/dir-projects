package com.jhulian.android.youtube.classic.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Runs innertube calls through a real, offscreen WebView's own `fetch()`
 * instead of a plain HTTP client.
 *
 * Home/Subscriptions/Shorts kept coming back `HTTP 200` with
 * `"logged_in":"0"` from [Innertube.post] no matter how closely its
 * headers were made to match a real browser (confirmed on a real device
 * across multiple rounds of header-consistency fixes, right down to
 * making the claimed browser identity and the actual User-Agent agree) -
 * which matches a widely documented YouTube anti-abuse behavior (see
 * yt-dlp's/NewPipe's "poToken"/attestation issue threads): Google's
 * signed-in web endpoints increasingly expect a proof-of-origin signal
 * that only a real browser engine actually running YouTube's own page can
 * produce, which no amount of hand-crafted headers on a plain HTTP client
 * can fake. A WebView *is* a real browser engine - same TLS stack, same
 * JS engine, same cookie jar [com.jhulian.android.youtube.classic.auth.LoginActivity]
 * already populated (WebView's `CookieManager` is one shared, process-wide
 * jar) - so running just these calls as an actual `fetch()` inside one
 * sidesteps the problem rather than trying to out-guess it further.
 *
 * Kept as a single long-lived instance ([Innertube] holds one, wired up
 * from [com.jhulian.android.youtube.classic.YtClassicApp]) rather than a
 * WebView per call - loading youtube.com once and reusing that same page
 * context for every subsequent `fetch()` is both cheaper and closer to how
 * a real browser tab actually behaves than tearing one down and rebuilding
 * it per request.
 */
class WebViewInnertubeBridge(context: Context) {

    private val appContext = context.applicationContext
    private val pending = ConcurrentHashMap<String, CancellableContinuation<String>>()
    private val nextId = AtomicLong(0)
    private var loadDeferred: CompletableDeferred<WebView>? = null

    private inner class Bridge {
        @JavascriptInterface
        fun onResult(id: String, text: String) {
            pending.remove(id)?.resume(text)
        }

        @JavascriptInterface
        fun onError(id: String, message: String) {
            pending.remove(id)?.resumeWithException(WebViewFetchException(message))
        }

        @JavascriptInterface
        fun onDiagnostic(message: String) {
            android.util.Log.d("WebViewInnertubeBridge", "page diagnostic: $message")
        }
    }

    private data class LiveConfig(val apiKey: String, val context: JSONObject, val clientVersion: String?)

    private var liveConfig: LiveConfig? = null

    /**
     * Pulls the real `INNERTUBE_API_KEY`/`INNERTUBE_CONTEXT` straight out of
     * the loaded youtube.com page's own `ytcfg` config - the exact values
     * that page's own JS uses to make this exact same browse() call -
     * instead of the hand-maintained constants in [Innertube]. Everything
     * else (real browser engine, real headers, real cookies) was already
     * ruled out on a real device: a fresh, still-valid cookie from the
     * user's own standalone browser, on the same device/network, still came
     * back "logged_in":"0" even through a genuine WebView fetch. A stale or
     * subtly-wrong hardcoded key/context is the remaining unverified
     * variable - this replaces it with the one source that can't be stale,
     * since the page loaded it moments ago for its own use. Cached for the
     * life of this WebView instance once found; returns null (caller falls
     * back to the hardcoded values) if `ytcfg` isn't there for any reason.
     */
    private suspend fun getLiveConfig(view: WebView): LiveConfig? {
        liveConfig?.let { return it }
        val raw = suspendCancellableCoroutine<String?> { cont ->
            view.evaluateJavascript(
                "(function(){try{" +
                    "if(typeof ytcfg==='undefined')return null;" +
                    "return {key:ytcfg.get('INNERTUBE_API_KEY'),ctx:ytcfg.get('INNERTUBE_CONTEXT')};" +
                    "}catch(e){return null;}})();",
                { result -> cont.resume(result) },
            )
        }
        if (raw == null || raw == "null") {
            android.util.Log.w("WebViewInnertubeBridge", "getLiveConfig: ytcfg unavailable, falling back to hardcoded key/context")
            return null
        }
        return try {
            val obj = JSONObject(raw)
            val key = obj.getString("key")
            val ctx = obj.getJSONObject("ctx")
            val clientVersion = ctx.optJSONObject("client")?.optString("clientVersion")
            LiveConfig(key, ctx, clientVersion).also {
                liveConfig = it
                android.util.Log.d("WebViewInnertubeBridge", "getLiveConfig: using live ytcfg (clientVersion=$clientVersion)")
            }
        } catch (e: Exception) {
            android.util.Log.w("WebViewInnertubeBridge", "getLiveConfig: failed to parse ytcfg result", e)
            null
        }
    }

    /**
     * Checks, from inside the WebView's already-loaded youtube.com page,
     * whether that page itself currently thinks it's signed in - `document
     * .cookie` reflects the live cookie jar at whatever moment this runs,
     * not a snapshot frozen at page load, so no reload is needed to get a
     * reading against a cookie captured after this WebView first loaded
     * (which is exactly what happened the first time this shipped: the
     * bridge's one-time page-load diagnostic had already fired, and been
     * cleared by `logcat -c`, before a since-refreshed cookie was ever
     * tested against it). [label] tags which call site this reading came
     * from. Redacted: booleans and a length, never cookie content.
     */
    private fun runDiagnostic(view: WebView, label: String) {
        view.evaluateJavascript(
            "(function(){try{" +
                "var avatar=!!document.querySelector('#avatar-btn, ytd-topbar-menu-button-renderer #avatar-btn');" +
                "var signIn=!!document.querySelector('a[href*=\"ServiceLogin\"], tp-yt-paper-button#sign-in-button');" +
                "var initialDataLoggedIn=(typeof ytInitialData!=='undefined')?JSON.stringify(ytInitialData).indexOf('\"logged_in\":\"1\"')!==-1:'ytInitialData undefined';" +
                "YtClassicBridge.onDiagnostic(" + JSONObject.quote(label) + "+': avatarBtnPresent='+avatar+' signInLinkPresent='+signIn+' initialDataLoggedIn1='+initialDataLoggedIn+' cookieLen='+document.cookie.length+' hasSAPISIDInJs='+(document.cookie.indexOf('SAPISID=')!==-1));" +
                "}catch(e){YtClassicBridge.onDiagnostic(" + JSONObject.quote(label) + "+': error: '+String(e));}})();",
            null,
        )
    }

    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi")
    private suspend fun ensureReady(): WebView {
        loadDeferred?.let { return it.await() }
        val deferred = CompletableDeferred<WebView>()
        loadDeferred = deferred

        lateinit var view: WebView
        view = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Matches WebView's real underlying engine (Chromium) - the
            // exact bug this whole class exists to route around was a
            // request claiming to be a browser it wasn't, so this WebView
            // must actually identify as the browser it really is rather
            // than repeating that mistake in a new place.
            settings.userAgentString = Innertube.WEBVIEW_USER_AGENT
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
            }
            addJavascriptInterface(Bridge(), "YtClassicBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(v: WebView, url: String?) {
                    runDiagnostic(v, "onLoad")
                    deferred.complete(view)
                }
            }
        }
        view.loadUrl("https://www.youtube.com")
        return deferred.await()
    }

    /**
     * POSTs [body] to `/youtubei/v1/[endpoint]` via a real `fetch()` running
     * inside the WebView's own youtube.com page, with `credentials:
     * 'include'` so it rides on whatever session the shared cookie jar
     * already holds - no cookie header is passed in by hand.
     * [fallbackAuthorizationHeader], if given, mirrors the `SAPISIDHASH` a
     * real youtube.com page computes client-side before making this same
     * call - used only when [getLiveConfig] can't supply a live one.
     *
     * When the page's own `ytcfg` is available, [body]'s `context` field
     * and the request's API key are replaced with the real, live ones that
     * page is actually using - see [getLiveConfig]'s kdoc for why. [body]
     * is mutated in place either way (its `context` field is always the one
     * actually sent).
     *
     * `Origin`/`Referer`/`User-Agent` are deliberately not set here even
     * though [Innertube.post]'s equivalents are - `fetch()` refuses to let
     * script override any of the three (they're "forbidden request
     * headers"), and the real values a genuine same-origin fetch from this
     * WebView produces are more authentic than anything hand-built anyway.
     */
    suspend fun postJson(
        endpoint: String,
        body: JSONObject,
        fallbackApiKey: String,
        fallbackClientVersion: String,
        fallbackAuthorizationHeader: String?,
    ): String = withContext(Dispatchers.Main.immediate) {
        val view = ensureReady()
        val live = getLiveConfig(view)
        if (live != null) {
            body.put("context", live.context)
        }
        val apiKey = live?.apiKey ?: fallbackApiKey
        val clientVersion = live?.clientVersion ?: fallbackClientVersion
        val bodyJson = body.toString()

        val id = nextId.incrementAndGet().toString()
        runDiagnostic(view, "before $endpoint #$id (liveConfig=${live != null})")
        suspendCancellableCoroutine { cont ->
            pending[id] = cont
            val headersJs = buildString {
                append("{'Content-Type':'application/json','X-YouTube-Client-Name':'1','X-YouTube-Client-Version':")
                append(JSONObject.quote(clientVersion))
                append(",'X-Goog-AuthUser':'0'")
                if (fallbackAuthorizationHeader != null) {
                    append(",'Authorization':").append(JSONObject.quote(fallbackAuthorizationHeader))
                }
                append('}')
            }
            val url = "https://www.youtube.com/youtubei/v1/$endpoint?key=$apiKey"
            val js = "fetch(${JSONObject.quote(url)},{method:'POST',credentials:'include',headers:$headersJs," +
                "body:${JSONObject.quote(bodyJson)}})" +
                ".then(function(r){return r.text();})" +
                ".then(function(t){YtClassicBridge.onResult(${JSONObject.quote(id)},t);})" +
                ".catch(function(e){YtClassicBridge.onError(${JSONObject.quote(id)},String(e));});"
            view.evaluateJavascript(js, null)
            cont.invokeOnCancellation { pending.remove(id) }
        }
    }
}

class WebViewFetchException(message: String) : Exception(message)
