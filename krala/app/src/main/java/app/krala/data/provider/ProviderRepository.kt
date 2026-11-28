package app.krala.data.provider

import app.krala.core.Ids
import app.krala.data.db.ProviderDao
import app.krala.data.db.ProviderEntity
import app.krala.data.db.UsageDao
import app.krala.data.settings.SettingsRepository
import app.krala.security.KeyVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** api key + custom headers. stored encrypted, see [KeyVault] */
@Serializable
data class ProviderSecrets(val apiKey: String = "", val headers: Map<String, String> = emptyMap())

data class Provider(val entity: ProviderEntity, val models: List<String>) {
    val id get() = entity.id
    val name get() = entity.name
    val firstModel: String? get() = entity.defaultModel?.takeIf { it.isNotBlank() } ?: models.firstOrNull()
}

data class ProviderDraft(
    val id: String? = null,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val headers: Map<String, String>,
    val models: List<String>,
    val defaultModel: String?,
    val contextSize: Int,
    val supportsVision: Boolean,
    val enabled: Boolean = true,
    val presetId: String? = null,
)

@Singleton
class ProviderRepository @Inject constructor(
    private val dao: ProviderDao,
    private val usage: UsageDao,
    private val vault: KeyVault,
    private val settings: SettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSer = ListSerializer(String.serializer())

    fun decodeModels(raw: String): List<String> = runCatching { json.decodeFromString(listSer, raw) }.getOrDefault(emptyList())
    fun encodeModels(list: List<String>): String = json.encodeToString(listSer, list)

    val providers: Flow<List<Provider>> = dao.observeAll().map { list -> list.map { Provider(it, decodeModels(it.models)) } }

    suspend fun list(): List<Provider> = dao.list().map { Provider(it, decodeModels(it.models)) }

    suspend fun get(id: String): Provider? = dao.get(id)?.takeIf { !it.deleted }?.let { Provider(it, decodeModels(it.models)) }

    fun secrets(e: ProviderEntity): ProviderSecrets =
        vault.decrypt(e.secretCipher)?.let { runCatching { json.decodeFromString(ProviderSecrets.serializer(), it) }.getOrNull() }
            ?: ProviderSecrets()

    fun encryptSecrets(s: ProviderSecrets): String = vault.encrypt(json.encodeToString(ProviderSecrets.serializer(), s))

    fun secretsJson(s: ProviderSecrets): String = json.encodeToString(ProviderSecrets.serializer(), s)
    fun parseSecrets(raw: String): ProviderSecrets? = runCatching { json.decodeFromString(ProviderSecrets.serializer(), raw) }.getOrNull()

    fun config(p: Provider): ProviderConfig {
        val s = secrets(p.entity)
        val preset = Presets.byId(p.entity.presetId)
        return ProviderConfig(
            id = p.id,
            name = p.name,
            baseUrl = p.entity.baseUrl,
            apiKey = s.apiKey.ifBlank { preset?.fixedKey ?: "" },
            headers = s.headers,
            models = p.models,
            defaultModel = p.entity.defaultModel,
            contextSize = p.entity.contextSize,
            supportsVision = p.entity.supportsVision,
        )
    }

    fun configFromDraft(d: ProviderDraft) = ProviderConfig(
        id = d.id ?: "draft",
        name = d.name,
        baseUrl = d.baseUrl,
        apiKey = d.apiKey.ifBlank { Presets.byId(d.presetId)?.fixedKey ?: "" },
        headers = d.headers,
        models = d.models,
        defaultModel = d.defaultModel,
        contextSize = d.contextSize,
        supportsVision = d.supportsVision,
    )

    suspend fun save(d: ProviderDraft): String {
        val now = System.currentTimeMillis()
        val existing = d.id?.let { dao.get(it) }
        val id = existing?.id ?: Ids.new()
        val priority = existing?.priority ?: ((dao.list().maxOfOrNull { it.priority } ?: -1) + 1)
        dao.upsert(
            ProviderEntity(
                id = id,
                name = d.name.trim().ifBlank { "provider" },
                baseUrl = d.baseUrl.trim().trimEnd('/'),
                models = encodeModels(d.models.map { it.trim() }.filter { it.isNotEmpty() }.distinct()),
                defaultModel = d.defaultModel?.trim()?.takeIf { it.isNotEmpty() },
                contextSize = d.contextSize.coerceIn(1_024, 10_000_000),
                priority = priority,
                enabled = d.enabled,
                presetId = d.presetId,
                supportsVision = d.supportsVision,
                secretCipher = encryptSecrets(ProviderSecrets(d.apiKey.trim(), d.headers)),
                rateLimitedUntil = existing?.rateLimitedUntil ?: 0,
                remainingRequests = existing?.remainingRequests,
                lastError = existing?.lastError,
                lastErrorAt = existing?.lastErrorAt ?: 0,
                lastLatencyMs = existing?.lastLatencyMs ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncedAt = existing?.syncedAt ?: 0,
            )
        )
        return id
    }

    suspend fun setModels(id: String, models: List<String>) {
        val e = dao.get(id) ?: return
        dao.upsert(e.copy(models = encodeModels(models), updatedAt = System.currentTimeMillis()))
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val e = dao.get(id) ?: return
        dao.upsert(e.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) = dao.softDelete(id, System.currentTimeMillis())

    suspend fun reorder(ids: List<String>) = dao.reorder(ids, System.currentTimeMillis())

    /** the built in no key default, added once on first launch */
    suspend fun seedIfNeeded() {
        val s = settings.current()
        if (s.presetsSeeded) return
        if (dao.countAll() == 0) {
            val p = Presets.free
            save(
                ProviderDraft(
                    name = p.name, baseUrl = p.baseUrl, apiKey = p.fixedKey ?: "", headers = p.headers,
                    models = p.models, defaultModel = p.models.first(), contextSize = p.contextSize,
                    supportsVision = p.vision, presetId = p.id,
                )
            )
        }
        settings.setPresetsSeeded()
    }

    // ---- rate limits + usage ----

    suspend fun markRateLimited(id: String, retryAfterMs: Long, remaining: Int?) =
        dao.setRateLimit(id, System.currentTimeMillis() + retryAfterMs.coerceIn(1_000, 24 * 3_600_000L), remaining)

    suspend fun markSuccess(id: String, remaining: Int?, latencyMs: Long) {
        dao.recordSuccess(id, remaining, latencyMs)
        // a success means any old limit is over
        dao.setRateLimit(id, 0, remaining)
    }

    suspend fun markError(id: String, error: String) = dao.recordError(id, error.take(500), System.currentTimeMillis())

    suspend fun recordUsage(
        p: ProviderConfig, success: Boolean, rateLimited: Boolean, promptTokens: Int, completionTokens: Int,
        latencyMs: Long, firstTokenMs: Long,
    ) = usage.record(
        p.id, p.name, LocalDate.now().toEpochDay(), success, rateLimited, promptTokens, completionTokens, latencyMs, firstTokenMs,
    )
}
