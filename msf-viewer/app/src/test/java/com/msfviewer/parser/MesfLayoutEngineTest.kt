package com.msfviewer.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class MesfLayoutEngineTest {

    @Test
    fun emptyUnitsProduceEmptyLayout() {
        val layout = MesfLayoutEngine.layout(emptyList())
        assertEquals(0, layout.rowCount)
        assertEquals(0, layout.widthUnits)
        assertEquals(0, layout.heightUnits)
    }

    @Test
    fun unitsNarrowerThanRowWidthStayOnOneRow() {
        val units = List(5) { MesfUnit(colorNumber = 1, size = 1) }
        val layout = MesfLayoutEngine.layout(units, rowWidthUnits = 16)
        assertEquals(1, layout.rowCount)
        assertEquals(5, layout.widthUnits)
        assertEquals(1, layout.heightUnits)
        assertEquals(listOf(0, 1, 2, 3, 4), layout.placedUnits.map { it.x })
        assertEquals(listOf(0, 0, 0, 0, 0), layout.placedUnits.map { it.y })
    }

    @Test
    fun wrapsToNewRowWhenRowWidthLimitHit() {
        // row width 4, five 2-unit-wide units -> 2 per row, wraps.
        val units = List(5) { MesfUnit(colorNumber = 1, size = 2) }
        val layout = MesfLayoutEngine.layout(units, rowWidthUnits = 4)
        assertEquals(3, layout.rowCount) // 2 + 2 + 1
        assertEquals(4, layout.widthUnits)
        assertEquals(6, layout.heightUnits) // 3 rows * height 2
        assertEquals(listOf(0, 2, 0, 2, 0), layout.placedUnits.map { it.x })
        assertEquals(listOf(0, 0, 2, 2, 4), layout.placedUnits.map { it.y })
    }

    @Test
    fun rowHeightIsTallestUnitInThatRow() {
        val units = listOf(
            MesfUnit(colorNumber = 1, size = 1),
            MesfUnit(colorNumber = 2, size = 5),
            MesfUnit(colorNumber = 3, size = 1),
        )
        val layout = MesfLayoutEngine.layout(units, rowWidthUnits = 4)
        // row0: just the size-1 unit (adding the size-5 one would exceed
        // the row width, so it wraps instead of cropping). row1: the
        // size-5 unit alone -- oversized, but never dropped, gets a row
        // to itself, taller than the row width limit. row2: the last
        // size-1 unit, wrapped again since row1 was already full.
        assertEquals(3, layout.rowCount)
        assertEquals(5, layout.widthUnits) // widest row is row1, width 5
        assertEquals(7, layout.heightUnits) // heights 1 + 5 + 1
    }

    @Test
    fun oversizedUnitNeverDroppedGetsItsOwnRow() {
        val units = listOf(MesfUnit(colorNumber = 1, size = 20))
        val layout = MesfLayoutEngine.layout(units, rowWidthUnits = 16)
        assertEquals(1, layout.placedUnits.size)
        assertEquals(20, layout.widthUnits)
        assertEquals(20, layout.heightUnits)
    }

    @Test
    fun everyUnitAppearsInOutput() {
        val units = (1..50).map { MesfUnit(colorNumber = it % 27, size = (it % 3) + 1) }
        val layout = MesfLayoutEngine.layout(units)
        assertEquals(units.size, layout.placedUnits.size)
    }
}
