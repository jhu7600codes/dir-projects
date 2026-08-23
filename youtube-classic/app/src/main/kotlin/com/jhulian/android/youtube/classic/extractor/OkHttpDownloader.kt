package com.jhulian.android.youtube.classic.extractor

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * The [Downloader] NewPipeExtractor requires to actually make HTTP calls -
 * the library ships no networking of its own, by design, so every scraping
 * client built on it plugs in one of these. Modeled directly on NewPipe's
 * own `DownloaderImpl`.
 */
class OkHttpDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = OkRequest.Builder()
            .method(
                httpMethod,
                dataToSend?.toRequestBody(null),
            )
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        // Always remove-then-add for every header NewPipeExtractor supplies,
        // matching NewPipe's own DownloaderImpl exactly. This matters for an
        // empty value list too: that means "clear whatever default we set
        // for this header" (e.g. dropping our own User-Agent so the
        // extractor's own value wins) - a single if/else on list size, as
        // an earlier version of this file had, silently skips that case and
        // leaves a stale header the request wasn't supposed to carry.
        for ((headerName, headerValues) in headers) {
            requestBuilder.removeHeader(headerName)
            for (value in headerValues) {
                requestBuilder.addHeader(headerName, value)
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val body = response.body
            val responseBodyToReturn = body?.string() ?: ""
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBodyToReturn,
                latestUrl,
            )
        }
    }

    companion object {
        // Matches NewPipe's own DownloaderImpl exactly - extraction relies on
        // YouTube's response shape for a given UA/client combination, so
        // reusing NewPipe's proven value rather than a similar-looking one
        // of our own avoids subtly different server-side behavior.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        val instance: OkHttpDownloader by lazy {
            OkHttpDownloader(
                OkHttpClient.Builder()
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}
