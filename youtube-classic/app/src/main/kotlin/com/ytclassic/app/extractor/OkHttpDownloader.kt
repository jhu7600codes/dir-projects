package com.ytclassic.app.extractor

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

        for ((headerName, headerValues) in headers) {
            if (headerValues.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (value in headerValues) {
                    requestBuilder.addHeader(headerName, value)
                }
            } else if (headerValues.size == 1) {
                requestBuilder.header(headerName, headerValues[0])
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
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

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
