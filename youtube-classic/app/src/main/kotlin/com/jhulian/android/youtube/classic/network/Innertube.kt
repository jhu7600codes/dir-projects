package com.jhulian.android.youtube.classic.network

import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Everything needed to call YouTube's internal `/youtubei/v1/` (innertube)
 * endpoints as a logged-in web client, authenticated purely off the cookie
 * jar [com.jhulian.android.youtube.classic.auth.SessionManager] captured from the WebView
 * login - no API key, no OAuth token.
 *
 * This is the same mechanism yt-dlp and PipePipe use for cookie-authenticated
 * write actions: Google's `SAPISIDHASH` scheme lets a request prove it holds
 * a valid `SAPISID` cookie for a given origin without sending the cookie
 * itself as a bearer token - see
 * https://developers.google.com/identity/sign-in/web/sign-in#calling_the_google_api
 * ("Authorization: SAPISIDHASH ...") for the scheme this reimplements.
 *
 * The exact innertube request bodies (`context`, endpoint params) are
 * reverse-engineered and YouTube changes them without notice; if a call
 * here starts failing, the client version/key below and the endpoint
 * payloads in [InnertubeActions]/[InnertubeFeedClient] are the first things
 * to re-check against a fresh browser network capture.
 */
object Innertube {

    // Public, unauthenticated "innertube API key" baked into every
    // youtube.com page load - not a secret, not tied to any account.
    private const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val CLIENT_VERSION = "2.20240701.00.00"
    private const val ORIGIN = "https://www.youtube.com"
    private const val BASE_URL = "https://www.youtube.com/youtubei/v1"
    private const val TAG = "Innertube"

    // One real, self-consistent browser identity, reused for both the
    // context body below and the actual outgoing headers in post(). This
    // used to be split: buildContext() claimed browserName "Chrome"/126
    // while both its own embedded "userAgent" field *and* the real
    // User-Agent header in post() were a Firefox 140 string - an
    // authenticated browse() call would come back HTTP 200 with
    // "logged_in":"0" (confirmed on a real device: cookies present,
    // SAPISID hash attached, still treated as signed-out) with no error to
    // catch, which is exactly what a server-side bot/fraud check silently
    // downgrading a self-contradictory client fingerprint looks like,
    // rather than a hard block. Matches NewPipeExtractor's own proven
    // OkHttpDownloader UA so there's only one browser identity in the app.
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

    // The identity [WebViewInnertubeBridge] actually runs as - real Chrome
    // desktop, matching a WebView's true underlying engine (Chromium)
    // rather than pretending to be a browser it isn't, which was the whole
    // bug the USER_AGENT/buildContext() split above existed to fix in the
    // first place. browse() calls route through the bridge (see
    // [postAuthenticated]) and must use this identity in their context
    // body to match; everything else here keeps using [USER_AGENT] above.
    const val WEBVIEW_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private const val WEBVIEW_BROWSER_VERSION = "126.0.0.0"

    private val client = OkHttpClient.Builder().build()
    private val jsonMedia = "application/json".toMediaType()
    private var webViewBridge: WebViewInnertubeBridge? = null

    /** Wired up once from [com.jhulian.android.youtube.classic.YtClassicApp]. */
    fun init(bridge: WebViewInnertubeBridge) {
        webViewBridge = bridge
    }

    // A sparse context (just clientName/clientVersion/hl/gl) is enough for
    // the simple write actions (like/subscribe/comment), but `browse` calls
    // - the ones behind Home/Subscriptions/Shorts - are reported to come
    // back content-free far more often with a minimal context than a real
    // browser's, which sends all of this. Matches the shape yt-dlp's WEB
    // client definition sends. [browserName]/[browserVersion]/[userAgent]
    // default to the plain-OkHttp identity ([USER_AGENT]); browse() calls
    // pass the WebView bridge's real Chrome identity instead, since that's
    // the actual engine issuing those requests - see [WEBVIEW_USER_AGENT].
    fun buildContext(
        browserName: String = "Firefox",
        browserVersion: String = "140.0",
        userAgent: String = "$USER_AGENT,gzip(gfe)",
    ): JSONObject = JSONObject().apply {
        put(
            "client",
            JSONObject().apply {
                put("clientName", "WEB")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
                put("platform", "DESKTOP")
                put("clientFormFactor", "UNKNOWN_FORM_FACTOR")
                put("browserName", browserName)
                put("browserVersion", browserVersion)
                put("osName", "Windows")
                put("osVersion", "10.0")
                put("userAgent", userAgent)
                put("originalUrl", ORIGIN)
                put("screenPixelDensity", 1)
                put("screenDensityFloat", 1)
                put("utcOffsetMinutes", 0)
            },
        )
        put(
            "user",
            JSONObject().apply {
                put("lockedSafetyMode", false)
            },
        )
    }

    /** [buildContext] pre-filled with the WebView bridge's real Chrome identity. */
    fun buildWebViewContext(): JSONObject = buildContext(
        browserName = "Chrome",
        browserVersion = WEBVIEW_BROWSER_VERSION,
        userAgent = "$WEBVIEW_USER_AGENT,gzip(gfe)",
    )

    /**
     * Google's SAPISIDHASH scheme: SHA1("<unix-seconds> <SAPISID> <origin>"),
     * sent as `Authorization: SAPISIDHASH <ts>_<hash>` alongside the cookie.
     * Proves the request holds the SAPISID cookie for this exact origin
     * without it needing to double as a bearer token.
     */
    private fun sapisidHash(sapisid: String): String {
        val timestamp = System.currentTimeMillis() / 1000
        val input = "$timestamp $sapisid $ORIGIN"
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestamp}_$hex"
    }

    private fun extractCookieValue(cookieHeader: String, name: String): String? =
        cookieHeader.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("=")

    /**
     * POSTs [body] to `/youtubei/v1/[endpoint]`, authenticated with
     * [cookieHeader] if present. Returns the parsed JSON response, or throws
     * on a non-2xx status / unparsable body.
     */
    suspend fun post(endpoint: String, body: JSONObject, cookieHeader: String?): JSONObject =
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url("$BASE_URL/$endpoint?key=$API_KEY")
                .post(body.toString().toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .header("Origin", ORIGIN)
                .header("X-Origin", ORIGIN)
                .header("X-Goog-AuthUser", "0")
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$ORIGIN/")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "same-origin")
                .header("Sec-Fetch-Site", "same-origin")

            if (!cookieHeader.isNullOrBlank()) {
                val sapisid = extractCookieValue(cookieHeader, "SAPISID")
                    ?: extractCookieValue(cookieHeader, "__Secure-3PAPISID")
                requestBuilder.header("Cookie", cookieHeader)
                if (sapisid != null) {
                    requestBuilder.header("Authorization", sapisidHash(sapisid))
                }
                // Google's server silently degrades an unauthenticated-looking
                // request to logged-out content instead of erroring (real
                // device report: browse() came back HTTP 200 with
                // "logged_in":"0" despite the app genuinely holding a signed-in
                // session) - there's no error to catch there, so the only way
                // to tell "cookie capture is broken" from "hash/session is
                // rejected server-side" apart is to see what was actually in
                // the outgoing request. Deliberately logs presence/length only,
                // never the cookie or hash values themselves - this is a real
                // credential, not just debug noise.
                fun has(name: String) = extractCookieValue(cookieHeader, name) != null
                android.util.Log.d(
                    TAG,
                    "auth for $endpoint: cookieLen=${cookieHeader.length}, " +
                        "hasSAPISID=${has("SAPISID")}, has3PAPISID=${has("__Secure-3PAPISID")}, " +
                        "has1PAPISID=${has("__Secure-1PAPISID")}, hasSID=${has("SID")}, " +
                        "has1PSID=${has("__Secure-1PSID")}, hasHSID=${has("HSID")}, hasSSID=${has("SSID")}, " +
                        "sapisidResolved=${sapisid != null}, deviceTimeMs=${System.currentTimeMillis()}",
                )
            } else {
                android.util.Log.d(TAG, "auth for $endpoint: no cookie header at all")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    android.util.Log.e(TAG, "POST $endpoint -> HTTP ${response.code}: ${text.take(500)}")
                    throw InnertubeException(response.code, text)
                }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            }
        }

    /**
     * Like [post], but for the browse() surfaces behind Home/Subscriptions/
     * Shorts specifically - see [WebViewInnertubeBridge]'s kdoc for why
     * those need a real browser engine rather than a plain HTTP client.
     * Falls back to [post] if the bridge hasn't been wired up via [init]
     * yet, which shouldn't happen outside of a broken app startup.
     */
    suspend fun postAuthenticated(endpoint: String, body: JSONObject, cookieHeader: String): JSONObject {
        val bridge = webViewBridge
        if (bridge == null) {
            android.util.Log.w(TAG, "postAuthenticated($endpoint): no WebView bridge wired up, falling back to plain post()")
            return post(endpoint, body, cookieHeader)
        }
        val sapisid = extractCookieValue(cookieHeader, "SAPISID")
            ?: extractCookieValue(cookieHeader, "__Secure-3PAPISID")
        val authHeader = sapisid?.let { sapisidHash(it) }
        android.util.Log.d(TAG, "postAuthenticated($endpoint): via WebView bridge, sapisidResolved=${sapisid != null}")
        val text = bridge.postJson(endpoint, body, API_KEY, CLIENT_VERSION, authHeader)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }
}

class InnertubeException(val httpCode: Int, val rawBody: String) :
    Exception("Innertube request failed with HTTP $httpCode")
