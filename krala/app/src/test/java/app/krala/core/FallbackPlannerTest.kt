package app.krala.core

import app.krala.core.FallbackPlanner.Candidate
import app.krala.core.FallbackPlanner.ErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackPlannerTest {
    private val now = 1_000_000L
    private fun c(id: String, priority: Int, enabled: Boolean = true, limitedUntil: Long = 0, hasModel: Boolean = true) =
        Candidate(id, priority, enabled, limitedUntil, hasModel)

    @Test
    fun ordersByPriority() {
        val order = FallbackPlanner.order(listOf(c("b", 1), c("c", 2), c("a", 0)), null, now)
        assertEquals(listOf("a", "b", "c"), order)
    }

    @Test
    fun preferredGoesFirst() {
        val order = FallbackPlanner.order(listOf(c("a", 0), c("b", 1), c("c", 2)), "c", now)
        assertEquals(listOf("c", "a", "b"), order)
    }

    @Test
    fun disabledAndModelLessAreSkipped() {
        val order = FallbackPlanner.order(listOf(c("a", 0, enabled = false), c("b", 1, hasModel = false), c("c", 2)), "a", now)
        assertEquals(listOf("c"), order)
    }

    @Test
    fun rateLimitedMoveToTheBackSoonestFirst() {
        val order = FallbackPlanner.order(
            listOf(c("a", 0, limitedUntil = now + 60_000), c("b", 1, limitedUntil = now + 5_000), c("c", 2)),
            null, now,
        )
        assertEquals(listOf("c", "b", "a"), order)
    }

    @Test
    fun preferredButLimitedIsNotFirst() {
        val order = FallbackPlanner.order(listOf(c("a", 0, limitedUntil = now + 1_000), c("b", 1)), "a", now)
        assertEquals(listOf("b", "a"), order)
    }

    @Test
    fun expiredLimitCountsAsFree() {
        val order = FallbackPlanner.order(listOf(c("a", 0, limitedUntil = now - 1), c("b", 1)), null, now)
        assertEquals(listOf("a", "b"), order)
    }

    @Test
    fun everythingLimitedStillReturnsAll() {
        val order = FallbackPlanner.order(listOf(c("a", 0, limitedUntil = now + 9), c("b", 1, limitedUntil = now + 3)), null, now)
        assertEquals(listOf("b", "a"), order)
    }

    @Test
    fun fallsBackOnErrorsButNotAfterStreamingOrCancel() {
        listOf(ErrorKind.RATE_LIMIT, ErrorKind.SERVER, ErrorKind.NETWORK, ErrorKind.AUTH, ErrorKind.NOT_FOUND, ErrorKind.EMPTY, ErrorKind.TIMEOUT, ErrorKind.BAD_REQUEST)
            .forEach { assertTrue(it.name, FallbackPlanner.shouldFallback(it, streamedAnything = false)) }
        assertFalse(FallbackPlanner.shouldFallback(ErrorKind.SERVER, streamedAnything = true))
        assertFalse(FallbackPlanner.shouldFallback(ErrorKind.CANCELLED, streamedAnything = false))
    }

    @Test
    fun classifiesHttpCodes() {
        assertEquals(ErrorKind.RATE_LIMIT, FallbackPlanner.classify(429))
        assertEquals(ErrorKind.AUTH, FallbackPlanner.classify(401))
        assertEquals(ErrorKind.AUTH, FallbackPlanner.classify(403))
        assertEquals(ErrorKind.NOT_FOUND, FallbackPlanner.classify(404))
        assertEquals(ErrorKind.SERVER, FallbackPlanner.classify(503))
        assertEquals(ErrorKind.BAD_REQUEST, FallbackPlanner.classify(400))
        assertEquals(ErrorKind.NETWORK, FallbackPlanner.classify(null))
    }

    @Test
    fun spotsRateLimitsHiddenInBodies() {
        assertTrue(FallbackPlanner.looksRateLimited("You exceeded your current quota"))
        assertTrue(FallbackPlanner.looksRateLimited("RESOURCE_EXHAUSTED"))
        assertTrue(FallbackPlanner.looksRateLimited("Rate limit reached for tokens per minute"))
        assertFalse(FallbackPlanner.looksRateLimited("invalid model"))
        assertFalse(FallbackPlanner.looksRateLimited(null))
    }
}
