package app.krala.data.provider

import app.krala.core.ChoiceContent
import app.krala.core.FallbackPlanner
import app.krala.core.FallbackPlanner.ErrorKind
import app.krala.core.RateLimits
import app.krala.core.SseLineParser
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emitAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StreamEvent {
    data class Opened(val headers: Map<String, String>) : StreamEvent
    data class Delta(val text: String) : StreamEvent
    data class Reasoning(val text: String) : StreamEvent
}

/**
 * Talks to any openai compatible endpoint. From responses only choices[0].message.content (or
 * choices[0].delta.content while streaming) is read; every other field is ignored.
 */
@Singleton
class OpenAiClient @Inject constructor(
    private val http: HttpClient,
    private val gemini: GeminiRoute,
    private val settings: app.krala.data.settings.SettingsRepository,
) {
    /** models that refused the thinking settings once: send them plain from then on */
    private val noThinkingConfig = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun url(cfg: ProviderConfig, path: String) = cfg.baseUrl.trim().trimEnd('/') + "/" + path

    private fun HttpRequestBuilder.auth(cfg: ProviderConfig) {
        if (cfg.apiKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${cfg.apiKey.trim()}")
        cfg.headers.forEach { (k, v) -> if (k.isNotBlank()) header(k.trim(), v) }
    }

    fun chatBody(model: String, messages: List<ApiMessage>, stream: Boolean, maxTokens: Int? = null, thinking: Map<String, Any>? = null): String =
        buildJsonObject {
            put("model", model)
            put("stream", stream)
            // gemini's openai endpoint hides thinking unless asked; the summaries come back in <thought> tags
            if (thinking != null) putJsonObject("extra_body") {
                putJsonObject("google") {
                    putJsonObject("thinking_config") {
                        thinking.forEach { (k, v) ->
                            when (v) {
                                is Boolean -> put(k, v)
                                is Number -> put(k, v)
                                else -> put(k, v.toString())
                            }
                        }
                    }
                }
            }
            if (maxTokens != null) put("max_tokens", maxTokens)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        put("role", m.role)
                        if (m.images.isEmpty()) {
                            put("content", m.text)
                        } else {
                            putJsonArray("content") {
                                if (m.text.isNotEmpty()) addJsonObject {
                                    put("type", "text"); put("text", m.text)
                                }
                                m.images.forEach { url ->
                                    addJsonObject {
                                        put("type", "image_url")
                                        putJsonObject("image_url") { put("url", url) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.toString()

    fun stream(cfg: ProviderConfig, model: String, messages: List<ApiMessage>): Flow<StreamEvent> = flow {
        var routed = gemini.route(cfg)
        val thinking = if (gemini.isGemini(cfg) && model !in noThinkingConfig) {
            app.krala.core.GeminiThinking.config(model, settings.current().geminiThinking)
        } else null
        var opened = false
        suspend fun attempt(c: ProviderConfig, t: Map<String, Any>?) =
            streamOnce(c, model, messages, t).collect { if (it is StreamEvent.Opened) opened = true; emit(it) }
        try {
            try {
                attempt(routed, thinking)
            } catch (e: ProviderException) {
                // google refused this location: go again through the proxy
                if (!opened && routed === cfg && gemini.isGemini(cfg) && gemini.isGeoBlock(e) && !gemini.vpnOn()) {
                    gemini.markBlocked()
                    routed = gemini.route(cfg, force = true)
                    attempt(routed, thinking)
                } else throw e
            }
        } catch (e: ProviderException) {
            // a model that doesn't take the thinking settings: once more without them
            if (!opened && thinking != null && e.kind == ErrorKind.BAD_REQUEST) {
                noThinkingConfig += model
                attempt(routed, null)
            } else throw e
        }
    }

    private fun streamOnce(cfg: ProviderConfig, model: String, messages: List<ApiMessage>, thinking: Map<String, Any>?): Flow<StreamEvent> = flow {
        try {
            http.preparePost(url(cfg, "chat/completions")) {
                auth(cfg)
                header(HttpHeaders.Accept, "text/event-stream, application/json")
                setBody(TextContent(chatBody(model, messages, stream = true, thinking = thinking), ContentType.Application.Json))
            }.execute { resp ->
                val headers = headerMap(resp)
                if (!resp.status.isSuccess()) throw httpError(resp.status.value, resp.bodyAsText(), headers)
                emit(StreamEvent.Opened(headers))
                val isJson = resp.contentType()?.match(ContentType.Application.Json) == true
                if (isJson) {
                    // provider ignored stream=true: one plain response
                    val body = resp.bodyAsText()
                    val content = ChoiceContent.fromResponse(body)
                        ?: throw ProviderException(ErrorKind.EMPTY, resp.status.value, ChoiceContent.errorMessage(body) ?: "empty response")
                    emit(StreamEvent.Delta(content))
                    return@execute
                }
                val channel = resp.bodyAsChannel()
                val parser = SseLineParser()
                suspend fun handle(data: String): Boolean {
                    val d = data.trim()
                    if (d == "[DONE]") return false
                    if (d.isEmpty()) return true
                    ChoiceContent.reasoningFromChunk(d)?.takeIf { it.isNotEmpty() }?.let { emit(StreamEvent.Reasoning(it)) }
                    val delta = ChoiceContent.fromChunk(d)
                    if (delta != null) {
                        if (delta.isNotEmpty()) emit(StreamEvent.Delta(delta))
                    } else if (d.contains("\"error\"")) {
                        val msg = ChoiceContent.errorMessage(d) ?: d.take(300)
                        val kind = if (FallbackPlanner.looksRateLimited(msg)) ErrorKind.RATE_LIMIT else ErrorKind.SERVER
                        throw ProviderException(kind, null, msg)
                    }
                    return true
                }
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    val data = parser.feed(line) ?: continue
                    if (!handle(data)) return@execute
                }
                parser.flush()?.let { handle(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            throw e
        } catch (e: Exception) {
            throw mapException(e)
        }
    }.flowOn(Dispatchers.IO)

    /** non streaming completion; returns choices[0].message.content */
    suspend fun complete(cfg: ProviderConfig, model: String, messages: List<ApiMessage>, maxTokens: Int? = null): String =
        geoRetry(cfg) { completeOnce(it, model, messages, maxTokens) }

    private suspend fun <T> geoRetry(cfg: ProviderConfig, call: suspend (ProviderConfig) -> T): T {
        val routed = gemini.route(cfg)
        return try {
            call(routed)
        } catch (e: ProviderException) {
            if (routed === cfg && gemini.isGemini(cfg) && gemini.isGeoBlock(e) && !gemini.vpnOn()) {
                gemini.markBlocked()
                call(gemini.route(cfg, force = true))
            } else throw e
        }
    }

    private suspend fun completeOnce(cfg: ProviderConfig, model: String, messages: List<ApiMessage>, maxTokens: Int?): String {
        try {
            val resp = http.post(url(cfg, "chat/completions")) {
                auth(cfg)
                setBody(TextContent(chatBody(model, messages, stream = false, maxTokens), ContentType.Application.Json))
            }
            val body = resp.bodyAsText()
            if (!resp.status.isSuccess()) throw httpError(resp.status.value, body, headerMap(resp))
            return ChoiceContent.fromResponse(body)?.let { app.krala.core.Think.answer(it) }
                ?: throw ProviderException(ErrorKind.EMPTY, resp.status.value, ChoiceContent.errorMessage(body) ?: "no choices[0].message.content in the response")
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            throw e
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    suspend fun listModels(cfg: ProviderConfig): List<String> = geoRetry(cfg) { listModelsOnce(it) }

    private suspend fun listModelsOnce(cfg: ProviderConfig): List<String> {
        try {
            val resp = http.get(url(cfg, "models")) { auth(cfg) }
            val body = resp.bodyAsText()
            if (!resp.status.isSuccess()) throw httpError(resp.status.value, body, headerMap(resp))
            return parseModels(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            throw e
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    fun parseModels(body: String): List<String> {
        val root = json.parseToJsonElement(body)
        val arr: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> (root["data"] ?: root["models"] ?: root["result"])?.let { it as? JsonArray } ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return arr.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> el.contentOrNull
                is JsonObject -> (el["id"] ?: el["name"])?.let { (it as? JsonPrimitive)?.contentOrNull }
                else -> null
            }
        }.map { it.removePrefix("models/") }.distinct().sorted()
    }

    suspend fun embed(cfg: ProviderConfig, model: String, inputs: List<String>): List<FloatArray> = geoRetry(cfg) { embedOnce(it, model, inputs) }

    private suspend fun embedOnce(cfg: ProviderConfig, model: String, inputs: List<String>): List<FloatArray> {
        try {
            val body = buildJsonObject {
                put("model", model)
                putJsonArray("input") { inputs.forEach { add(it) } }
            }.toString()
            val resp = http.post(url(cfg, "embeddings")) {
                auth(cfg)
                setBody(TextContent(body, ContentType.Application.Json))
            }
            val text = resp.bodyAsText()
            if (!resp.status.isSuccess()) throw httpError(resp.status.value, text, headerMap(resp))
            val data = json.parseToJsonElement(text).jsonObject["data"]?.jsonArray ?: return emptyList()
            return data.map { item ->
                val vec = item.jsonObject["embedding"]?.jsonArray ?: JsonArray(emptyList())
                FloatArray(vec.size) { i -> (vec[i] as JsonPrimitive).doubleOrNull?.toFloat() ?: 0f }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            throw e
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    /** checks /models, and when a provider has no /models, a one token completion */
    suspend fun test(cfg: ProviderConfig): TestResult {
        val start = System.nanoTime()
        fun elapsed() = (System.nanoTime() - start) / 1_000_000
        return try {
            val models = listModels(cfg)
            TestResult(true, elapsed(), 200, models, "ok, ${models.size} models")
        } catch (e: ProviderException) {
            if (e.httpCode == 404 || e.httpCode == 405) {
                val model = cfg.firstModel
                    ?: return TestResult(false, elapsed(), e.httpCode, emptyList(), "no /models endpoint and no model to try")
                try {
                    val start2 = System.nanoTime()
                    complete(cfg, model, listOf(ApiMessage("user", "ping")), maxTokens = 1)
                    TestResult(true, (System.nanoTime() - start2) / 1_000_000, 200, emptyList(), "ok (no /models endpoint, chat works)")
                } catch (e2: ProviderException) {
                    TestResult(false, elapsed(), e2.httpCode, emptyList(), describe(e2))
                }
            } else {
                TestResult(false, elapsed(), e.httpCode, emptyList(), describe(e))
            }
        }
    }

    fun describe(e: ProviderException): String = buildString {
        append(
            when (e.kind) {
                ErrorKind.RATE_LIMIT -> "rate limited"
                ErrorKind.AUTH -> "key rejected"
                ErrorKind.NOT_FOUND -> "not found (check the base url / model)"
                ErrorKind.BAD_REQUEST -> "bad request"
                ErrorKind.SERVER -> "server error"
                ErrorKind.NETWORK -> "network error"
                ErrorKind.TIMEOUT -> "timed out"
                ErrorKind.EMPTY -> "empty response"
                ErrorKind.CANCELLED -> "cancelled"
                ErrorKind.UNKNOWN -> "error"
            }
        )
        e.httpCode?.let { append(" (http ").append(it).append(')') }
        if (e.message.isNotBlank()) append(": ").append(e.message.take(400))
    }

    private fun headerMap(resp: HttpResponse): Map<String, String> =
        resp.headers.entries().associate { (k, v) -> k.lowercase() to v.joinToString(",") }

    private fun httpError(code: Int, body: String, headers: Map<String, String>): ProviderException {
        val msg = ChoiceContent.errorMessage(body) ?: body.take(400).ifBlank { "http $code" }
        var kind = FallbackPlanner.classify(code)
        if (kind != ErrorKind.RATE_LIMIT && code in listOf(400, 403) && FallbackPlanner.looksRateLimited(msg)) kind = ErrorKind.RATE_LIMIT
        val retry = if (kind == ErrorKind.RATE_LIMIT) RateLimits.retryAfterMs(headers, System.currentTimeMillis()) else null
        return ProviderException(kind, code, msg, retry, headers)
    }

    private fun mapException(e: Exception): ProviderException = when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            ProviderException(ErrorKind.TIMEOUT, null, e.message ?: "timed out", cause = e)
        is IOException -> ProviderException(ErrorKind.NETWORK, null, e.message ?: e.javaClass.simpleName, cause = e)
        else -> ProviderException(ErrorKind.UNKNOWN, null, e.message ?: e.javaClass.simpleName, cause = e)
    }
}
