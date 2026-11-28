package app.krala.voice

import app.krala.data.provider.ProviderConfig
import app.krala.data.provider.ProviderRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speech to text through a provider's openai compatible /audio/transcriptions (whisper).
 * Used when the phone has no working speech recognizer. Language is detected automatically.
 */
@Singleton
class Whisper @Inject constructor(private val http: HttpClient, private val providers: ProviderRepository) {
    /** a provider + model that can transcribe, or null */
    suspend fun target(): Pair<ProviderConfig, String>? {
        val list = providers.list().filter { it.entity.enabled }
        for (p in list) {
            val m = p.models.firstOrNull { it.contains("whisper", true) || it.contains("transcribe", true) }
            val model = m ?: when {
                "api.groq.com" in p.entity.baseUrl -> "whisper-large-v3-turbo"
                "api.openai.com" in p.entity.baseUrl -> "whisper-1"
                else -> null
            } ?: continue
            val cfg = providers.config(p)
            if (cfg.apiKey.isBlank()) continue
            return cfg to model
        }
        return null
    }

    suspend fun transcribe(file: File): String {
        val (cfg, model) = target() ?: error("no whisper provider")
        val resp = http.submitFormWithBinaryData(
            url = cfg.baseUrl.trimEnd('/') + "/audio/transcriptions",
            formData = formData {
                append("model", model)
                append("response_format", "json")
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "audio/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=\"talk.m4a\"")
                })
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer ${cfg.apiKey}")
            cfg.headers.forEach { (k, v) -> header(k, v) }
        }
        val body = resp.bodyAsText()
        if (!resp.status.isSuccess()) error("${cfg.name}: http ${resp.status.value} ${body.take(160)}")
        val o = runCatching { Json.parseToJsonElement(body) as JsonObject }.getOrNull()
        return ((o?.get("text") as? JsonPrimitive)?.contentOrNull ?: body).trim()
    }
}
