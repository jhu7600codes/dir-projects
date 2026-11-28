package app.krala.data.provider

import app.krala.core.FallbackPlanner
import app.krala.core.Tokens
import javax.inject.Inject
import javax.inject.Singleton

data class Attempt(val config: ProviderConfig, val model: String)

class AllProvidersFailed(val errors: List<Pair<String, ProviderException>>) :
    Exception(errors.lastOrNull()?.second?.message ?: "no provider is set up")

/**
 * Runs a request down the provider priority list, skipping to the next one on errors and rate
 * limits, and keeps the rate limit + usage bookkeeping.
 */
@Singleton
class ProviderRouter @Inject constructor(
    private val repo: ProviderRepository,
    private val client: OpenAiClient,
) {
    suspend fun attempts(preferredId: String?, preferredModel: String?, needsVision: Boolean = false): List<Attempt> {
        val providers = repo.list()
        val now = System.currentTimeMillis()
        val order = FallbackPlanner.order(
            providers.map {
                FallbackPlanner.Candidate(
                    id = it.id,
                    priority = it.entity.priority,
                    enabled = it.entity.enabled,
                    rateLimitedUntil = it.entity.rateLimitedUntil,
                    hasModel = (if (it.id == preferredId) preferredModel ?: it.firstModel else it.firstModel) != null,
                )
            },
            preferredId,
            now,
        )
        val byId = providers.associateBy { it.id }
        var list = order.mapNotNull { id ->
            val p = byId[id] ?: return@mapNotNull null
            val model = (if (id == preferredId) preferredModel?.takeIf { it.isNotBlank() } else null) ?: p.firstModel ?: return@mapNotNull null
            Attempt(repo.config(p), model)
        }
        if (needsVision && list.size > 1) {
            val first = list.first().takeIf { it.config.id == preferredId }
            val rest = (if (first != null) list.drop(1) else list).sortedBy { if (it.config.supportsVision) 0 else 1 }
            list = listOfNotNull(first) + rest
        }
        return list
    }

    suspend fun onError(a: Attempt, e: ProviderException) {
        repo.markError(a.config.id, client.describe(e))
        if (e.isRateLimit) {
            repo.markRateLimited(a.config.id, e.retryAfterMs ?: 30_000, app.krala.core.RateLimits.remainingRequests(e.headers))
        }
    }

    /** non streaming request with fallback. returns the text and who answered. */
    suspend fun complete(
        preferredId: String?,
        preferredModel: String?,
        messages: List<ApiMessage>,
        maxTokens: Int? = null,
    ): Pair<String, Attempt> {
        val errors = mutableListOf<Pair<String, ProviderException>>()
        val promptTokens = messages.sumOf { Tokens.estimate(it.text) }
        for (a in attempts(preferredId, preferredModel)) {
            val start = System.currentTimeMillis()
            try {
                val text = client.complete(a.config, a.model, messages, maxTokens)
                val latency = System.currentTimeMillis() - start
                repo.markSuccess(a.config.id, null, latency)
                repo.recordUsage(a.config, true, false, promptTokens, Tokens.estimate(text), latency, latency)
                return text to a
            } catch (e: ProviderException) {
                onError(a, e)
                repo.recordUsage(a.config, false, e.isRateLimit, promptTokens, 0, 0, 0)
                errors += a.config.name to e
                if (!FallbackPlanner.shouldFallback(e.kind, streamedAnything = false)) break
            }
        }
        throw AllProvidersFailed(errors)
    }

    fun describe(e: ProviderException) = client.describe(e)
}
