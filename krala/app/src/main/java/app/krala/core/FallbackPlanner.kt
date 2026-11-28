package app.krala.core

/**
 * Picks the order providers are tried in, and whether an error should move on to the next one.
 * Pure logic so it can be unit tested.
 */
object FallbackPlanner {
    data class Candidate(
        val id: String,
        val priority: Int,
        val enabled: Boolean,
        val rateLimitedUntil: Long,
        val hasModel: Boolean,
    )

    enum class ErrorKind { RATE_LIMIT, AUTH, NOT_FOUND, BAD_REQUEST, SERVER, NETWORK, TIMEOUT, EMPTY, CANCELLED, UNKNOWN }

    /**
     * Enabled providers with a model, preferred one first, then by priority. Providers that are
     * rate limited right now go to the back (soonest free first) instead of being dropped, so a
     * request still goes out when everything is limited.
     */
    fun order(candidates: List<Candidate>, preferredId: String?, now: Long): List<String> {
        val usable = candidates.filter { it.enabled && it.hasModel }
        val (limited, free) = usable.partition { it.rateLimitedUntil > now }
        val freeSorted = free.sortedWith(compareBy<Candidate> { if (it.id == preferredId) 0 else 1 }.thenBy { it.priority })
        val limitedSorted = limited.sortedWith(compareBy<Candidate> { it.rateLimitedUntil }.thenBy { it.priority })
        // the preferred provider stays first unless it is limited
        return (freeSorted + limitedSorted).map { it.id }
    }

    /**
     * Should the next provider be tried? Never after the user cancelled, and never once part of
     * the answer has streamed (the reply would be stitched from two models).
     */
    fun shouldFallback(kind: ErrorKind, streamedAnything: Boolean): Boolean {
        if (kind == ErrorKind.CANCELLED) return false
        if (streamedAnything) return false
        return true
    }

    fun classify(httpCode: Int?): ErrorKind = when (httpCode) {
        null -> ErrorKind.NETWORK
        429 -> ErrorKind.RATE_LIMIT
        401, 403 -> ErrorKind.AUTH
        404 -> ErrorKind.NOT_FOUND
        408 -> ErrorKind.TIMEOUT
        400, 413, 415, 422 -> ErrorKind.BAD_REQUEST
        in 500..599 -> ErrorKind.SERVER
        else -> ErrorKind.UNKNOWN
    }

    /** some providers say "rate limit" / "quota" in a 400 / 403 body instead of using 429 */
    fun looksRateLimited(body: String?): Boolean {
        val b = body?.lowercase() ?: return false
        return listOf("rate limit", "rate_limit", "ratelimit", "quota", "too many requests", "resource_exhausted", "tokens per minute")
            .any { it in b }
    }
}
