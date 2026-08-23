package com.ytclassic.app.network

import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Everything needed to call YouTube's internal `/youtubei/v1/*` (innertube)
 * endpoints as a logged-in web client, authenticated purely off the cookie
 * jar [com.ytclassic.app.auth.SessionManager] captured from the WebView
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

    private val client = OkHttpClient.Builder().build()
    private val jsonMedia = "application/json".toMediaType()

    fun buildContext(): JSONObject = JSONObject().apply {
        put(
            "client",
            JSONObject().apply {
                put("clientName", "WEB")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
            },
        )
    }

    /**
     * Google's SAPISIDHASH scheme: SHA1("<unix-seconds> <SAPISID> <origin>"),
     * sent as `Authorization: SAPISIDHASH <ts>_<hash>` alongside the cookie.
     * Proves the request holds the SAPISID cookie for this exact origin
     * without it needing to double as a bearer token.
     */
    private fun sapisidHash(cookieHeader: String, sapisid: String): String {
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

            if (!cookieHeader.isNullOrBlank()) {
                val sapisid = extractCookieValue(cookieHeader, "SAPISID")
                    ?: extractCookieValue(cookieHeader, "__Secure-3PAPISID")
                requestBuilder.header("Cookie", cookieHeader)
                if (sapisid != null) {
                    requestBuilder.header("Authorization", sapisidHash(cookieHeader, sapisid))
                }
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw InnertubeException(response.code, text)
                }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            }
        }
}

class InnertubeException(val httpCode: Int, val rawBody: String) :
    Exception("Innertube request failed with HTTP $httpCode")
