package app.krala.core

/** Parses the many ways providers say "slow down". */
object RateLimits {
    /**
     * Parses "30", "1.5", "250ms", "2m59.56s", "1h2m", "6m0s" into millis. Returns null when the
     * value isn't a duration.
     */
    fun parseDuration(raw: String?): Long? {
        val v = raw?.trim()?.lowercase() ?: return null
        if (v.isEmpty()) return null
        v.toDoubleOrNull()?.let { return (it * 1000).toLong() }
        val re = Regex("""(\d+(?:\.\d+)?)(ms|h|m|s)""")
        var total = 0.0
        var pos = 0
        var any = false
        for (m in re.findAll(v)) {
            if (m.range.first != pos) return null
            val n = m.groupValues[1].toDouble()
            total += when (m.groupValues[2]) {
                "ms" -> n
                "s" -> n * 1000
                "m" -> n * 60_000
                "h" -> n * 3_600_000
                else -> 0.0
            }
            pos = m.range.last + 1
            any = true
        }
        if (!any || pos != v.length) return null
        return total.toLong()
    }

    /**
     * How long to back off, from response headers. Header names must be lowercase. Falls back to
     * [defaultMs] when nothing useful is there.
     */
    fun retryAfterMs(headers: Map<String, String>, nowMs: Long, defaultMs: Long = 30_000): Long {
        headers["retry-after-ms"]?.toLongOrNull()?.let { return it }
        headers["retry-after"]?.let { ra ->
            parseDuration(ra)?.let { return it }
            parseHttpDate(ra)?.let { return (it - nowMs).coerceAtLeast(1_000) }
        }
        val reset = listOf(
            "x-ratelimit-reset-requests", "x-ratelimit-reset-tokens", "x-ratelimit-reset",
            "x-ratelimit-reset-requests-minute", "x-ratelimit-reset-tokens-minute",
        ).firstNotNullOfOrNull { headers[it] }
        if (reset != null) {
            val n = reset.toDoubleOrNull()
            if (n != null && n > 1_000_000_000) {
                // unix timestamp (seconds or millis)
                val ms = if (n > 1e12) n.toLong() else (n * 1000).toLong()
                return (ms - nowMs).coerceAtLeast(1_000)
            }
            parseDuration(reset)?.let { return it.coerceAtLeast(1_000) }
        }
        return defaultMs
    }

    /** remaining request count if the provider reports one */
    fun remainingRequests(headers: Map<String, String>): Int? =
        listOf("x-ratelimit-remaining-requests", "x-ratelimit-remaining", "x-ratelimit-remaining-requests-day")
            .firstNotNullOfOrNull { headers[it]?.trim()?.toIntOrNull() }

    private fun parseHttpDate(v: String): Long? = try {
        java.time.ZonedDateTime.parse(v, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}
