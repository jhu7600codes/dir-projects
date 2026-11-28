package app.krala.core

import app.krala.core.MemoryBudget.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryBudgetTest {
    private fun e(id: String, tokens: Int, at: Long, archive: Boolean = false) = Entry(id, tokens, at, archive)

    @Test
    fun nothingToDoUnderSixtyPercent() {
        val entries = listOf(e("a", 2_000, 1), e("b", 3_000, 2))
        assertFalse(MemoryBudget.needsCondense(entries, 10_000))
        assertNull(MemoryBudget.plan(entries, 10_000))
    }

    @Test
    fun exactlySixtyPercentIsStillFine() {
        assertNull(MemoryBudget.plan(listOf(e("a", 6_000, 1)), 10_000))
    }

    @Test
    fun triggersPastSixtyPercent() {
        assertTrue(MemoryBudget.needsCondense(listOf(e("a", 6_001, 1)), 10_000))
    }

    @Test
    fun mergesOldestFirstUntilUnderTarget() {
        val entries = listOf(e("new", 2_000, 50), e("old", 2_000, 10), e("mid", 2_000, 30), e("older", 1_000, 5))
        // 7000 > 6000 trigger, target 4000: drop older (6000 left), old (4000 left) -> done
        val plan = MemoryBudget.plan(entries, 10_000)
        assertNotNull(plan)
        assertEquals(listOf("older", "old"), plan!!.toMerge)
        assertEquals(2_000, plan.archiveBudget)
    }

    @Test
    fun archiveIsNeverPickedForMerging() {
        val entries = listOf(e("archive", 3_000, 1, archive = true), e("a", 2_000, 2), e("b", 2_000, 3))
        val plan = MemoryBudget.plan(entries, 10_000)!!
        assertFalse("archive" in plan.toMerge)
        assertTrue(plan.toMerge.isNotEmpty())
    }

    @Test
    fun alwaysMakesProgress() {
        // a single huge entry with no archive still gets merged (alone)
        val plan = MemoryBudget.plan(listOf(e("huge", 9_000, 1)), 10_000)!!
        assertEquals(listOf("huge"), plan.toMerge)
    }

    @Test
    fun onlyArchiveOverBudgetReCondensesIt() {
        val plan = MemoryBudget.plan(listOf(e("archive", 7_000, 1, archive = true)), 10_000)!!
        assertTrue(plan.toMerge.isEmpty())
    }

    @Test
    fun zeroContextNeverCondenses() {
        assertNull(MemoryBudget.plan(listOf(e("a", 1_000_000, 1)), 0))
    }

    @Test
    fun ties() {
        val plan = MemoryBudget.plan(listOf(e("b", 4_000, 1), e("a", 4_000, 1)), 10_000)!!
        assertEquals("a", plan.toMerge.first())
    }
}
