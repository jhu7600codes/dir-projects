package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateLimitsTest {
    @Test
    fun parsesDurations() {
        assertEquals(30_000L, RateLimits.parseDuration("30"))
        assertEquals(1_500L, RateLimits.parseDuration("1.5"))
        assertEquals(250L, RateLimits.parseDuration("250ms"))
        assertEquals(179_560L, RateLimits.parseDuration("2m59.56s"))
        assertEquals(3_720_000L, RateLimits.parseDuration("1h2m"))
        assertEquals(360_000L, RateLimits.parseDuration("6m0s"))
        assertNull(RateLimits.parseDuration("soon"))
        assertNull(RateLimits.parseDuration(""))
    }

    @Test
    fun retryAfterPrefersExplicitHeaders() {
        val now = 1_700_000_000_000L
        assertEquals(12_000L, RateLimits.retryAfterMs(mapOf("retry-after" to "12"), now))
        assertEquals(800L, RateLimits.retryAfterMs(mapOf("retry-after-ms" to "800", "retry-after" to "12"), now))
        assertEquals(7_660L, RateLimits.retryAfterMs(mapOf("x-ratelimit-reset-requests" to "7.66s"), now))
        assertEquals(30_000L, RateLimits.retryAfterMs(emptyMap(), now))
    }

    @Test
    fun unixResetTimestamps() {
        val now = 1_700_000_000_000L
        assertEquals(20_000L, RateLimits.retryAfterMs(mapOf("x-ratelimit-reset" to "1700000020"), now))
    }

    @Test
    fun remaining() {
        assertEquals(42, RateLimits.remainingRequests(mapOf("x-ratelimit-remaining-requests" to "42")))
        assertNull(RateLimits.remainingRequests(mapOf("x-other" to "1")))
    }
}
