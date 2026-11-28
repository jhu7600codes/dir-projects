package app.krala.core

import app.krala.core.ContextWindow.Item
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowTest {
    @Test
    fun keepsEverythingWhenItFits() {
        val items = List(5) { Item(100) }
        assertEquals(listOf(0, 1, 2, 3, 4), ContextWindow.fit(items, 10_000))
    }

    @Test
    fun dropsOldestButKeepsTheFirstMessage() {
        // budget = 4000 - 600 reserve = 3400
        val items = listOf(Item(1_000), Item(1_000), Item(1_000), Item(1_000), Item(1_000))
        val kept = ContextWindow.fit(items, 4_000)
        assertEquals(0, kept.first())
        assertEquals(4, kept.last())
        assertEquals(listOf(0, 3, 4), kept)
    }

    @Test
    fun newestMessageAlwaysKept() {
        val kept = ContextWindow.fit(listOf(Item(10), Item(50_000)), 4_000)
        assertEquals(listOf(0, 1), kept)
    }

    @Test
    fun withoutPinning() {
        val items = listOf(Item(1_000), Item(1_000), Item(1_000), Item(1_000))
        assertEquals(listOf(1, 2, 3), ContextWindow.fit(items, 4_000, pinnedFirst = false))
    }
}
