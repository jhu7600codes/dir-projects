package app.krala.data.web

import app.krala.core.Web
import app.krala.data.settings.SettingsRepository
import app.krala.security.KeyVault
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class WebFindings(val query: String, val sources: List<Web.Result>, val section: String)

/**
 * Client side web search: the app searches, reads the top pages and hands krala the text as a
 * --web-- section for that one request. No tool calls; the model only reads what it is given.
 */
@Singleton
class WebSearch @Inject constructor(
    private val http: HttpClient,
    private val settings: SettingsRepository,
    private val vault: KeyVault,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val browserUa = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    suspend fun run(message: String): WebFindings? {
        val q = Web.query(message)
        if (q.isBlank()) return null
        val s = settings.current()
        val key = vault.decrypt(s.webKeyCipher).orEmpty()
        val results = runCatching {
            when (s.webProvider) {
                "brave" -> if (key.isNotBlank()) brave(q, key) else duckDuckGo(q)
                "tavily" -> if (key.isNotBlank()) tavily(q, key) else duckDuckGo(q)
                "searxng" -> if (s.webSearxUrl.isNotBlank()) searx(q, s.webSearxUrl) else duckDuckGo(q)
                else -> duckDuckGo(q)
            }
        }.getOrElse { emptyList() }.ifEmpty { runCatching { wikipedia(q) }.getOrDefault(emptyList()) }
        if (results.isEmpty()) return null

        val top = results.take(5)
        val pages = coroutineScope {
            top.mapIndexed { i, r ->
                async {
                    val body = if (i < s.webPages) withTimeoutOrNull(8_000) { runCatching { page(r.url) }.getOrNull() } else null
                    r to (body ?: r.snippet)
                }
            }.awaitAll()
        }
        return WebFindings(q, top, Web.section(q, LocalDate.now().toString(), pages))
    }

    private suspend fun duckDuckGo(q: String): List<Web.Result> {
        val resp = http.get("https://html.duckduckgo.com/html/") {
            parameter("q", q)
            header("User-Agent", browserUa)
            header("Accept-Language", "en-US,en;q=0.8,ru;q=0.6")
        }
        if (!resp.status.isSuccess()) return emptyList()
        return Web.parseDuckDuckGo(resp.bodyAsText())
    }

    private suspend fun brave(q: String, key: String): List<Web.Result> {
        val resp = http.get("https://api.search.brave.com/res/v1/web/search") {
            parameter("q", q); parameter("count", 8)
            header("X-Subscription-Token", key); header("Accept", "application/json")
        }
        if (!resp.status.isSuccess()) error("brave: http ${resp.status.value}")
        val web = json.parseToJsonElement(resp.bodyAsText()).jsonObject["web"]?.jsonObject ?: return emptyList()
        return web["results"]?.jsonArray.orEmpty().mapNotNull { r -> (r as? JsonObject)?.let { result(it, "title", "url", "description") } }
    }

    private suspend fun tavily(q: String, key: String): List<Web.Result> {
        val resp = http.post("https://api.tavily.com/search") {
            setBody(TextContent(buildJsonObject { put("api_key", key); put("query", q); put("max_results", 6) }.toString(), ContentType.Application.Json))
        }
        if (!resp.status.isSuccess()) error("tavily: http ${resp.status.value}")
        return json.parseToJsonElement(resp.bodyAsText()).jsonObject["results"]?.jsonArray.orEmpty()
            .mapNotNull { r -> (r as? JsonObject)?.let { result(it, "title", "url", "content") } }
    }

    private suspend fun searx(q: String, base: String): List<Web.Result> {
        val resp = http.get(base.trimEnd('/') + "/search") { parameter("q", q); parameter("format", "json") }
        if (!resp.status.isSuccess()) error("searxng: http ${resp.status.value}")
        return json.parseToJsonElement(resp.bodyAsText()).jsonObject["results"]?.jsonArray.orEmpty()
            .mapNotNull { r -> (r as? JsonObject)?.let { result(it, "title", "url", "content") } }.take(8)
    }

    /** last resort that never needs a key */
    private suspend fun wikipedia(q: String): List<Web.Result> {
        val lang = if (app.krala.core.VoiceScript.isCyrillic(q)) "ru" else "en"
        val resp = http.get("https://$lang.wikipedia.org/w/api.php") {
            parameter("action", "query"); parameter("list", "search"); parameter("srsearch", q)
            parameter("format", "json"); parameter("srlimit", 5)
        }
        if (!resp.status.isSuccess()) return emptyList()
        val hits = json.parseToJsonElement(resp.bodyAsText()).jsonObject["query"]?.jsonObject?.get("search")?.jsonArray.orEmpty()
        return hits.mapNotNull { h ->
            val o = h as? JsonObject ?: return@mapNotNull null
            val title = (o["title"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            Web.Result(title, "https://$lang.wikipedia.org/wiki/" + title.replace(' ', '_'), Web.text((o["snippet"] as? JsonPrimitive)?.contentOrNull.orEmpty()))
        }
    }

    private suspend fun page(url: String): String? {
        val resp: HttpResponse = http.get(url) {
            header("User-Agent", browserUa)
            header("Accept", "text/html,application/xhtml+xml")
        }
        if (!resp.status.isSuccess()) return null
        val type = resp.headers["Content-Type"].orEmpty()
        if (type.isNotBlank() && !type.contains("html") && !type.contains("text")) return null
        val bytes = resp.bodyAsChannel().readRemaining(400_000).readByteArray()
        return Web.readable(String(bytes, Charsets.UTF_8), 2_000).takeIf { it.length > 80 }
    }

    private fun result(o: JsonObject, t: String, u: String, d: String): Web.Result? {
        val url = (o[u] as? JsonPrimitive)?.contentOrNull ?: return null
        return Web.Result(
            Web.text((o[t] as? JsonPrimitive)?.contentOrNull ?: Web.domain(url)),
            url,
            Web.text((o[d] as? JsonPrimitive)?.contentOrNull.orEmpty()),
        )
    }
}
