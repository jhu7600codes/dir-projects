package app.krala.data.provider

import app.krala.core.FallbackPlanner.ErrorKind

/** everything needed to talk to one openai compatible endpoint */
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val headers: Map<String, String>,
    val models: List<String>,
    val defaultModel: String?,
    val contextSize: Int,
    val supportsVision: Boolean,
) {
    val firstModel: String? get() = defaultModel?.takeIf { it.isNotBlank() } ?: models.firstOrNull()
}

/** one chat message as sent to the api. images are data: urls */
data class ApiMessage(val role: String, val text: String, val images: List<String> = emptyList())

class ProviderException(
    val kind: ErrorKind,
    val httpCode: Int?,
    override val message: String,
    val retryAfterMs: Long? = null,
    val headers: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isRateLimit get() = kind == ErrorKind.RATE_LIMIT
}

data class TestResult(
    val ok: Boolean,
    val latencyMs: Long,
    val httpCode: Int?,
    val models: List<String>,
    val detail: String,
)
