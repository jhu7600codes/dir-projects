package com.msfviewer.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class MesfColorTableTest {

    @Test
    fun baseColorsAreExact() {
        assertEquals(0x000000, MesfColorTable.colorFor(0))
        assertEquals(0xFF0000, MesfColorTable.colorFor(1))
        assertEquals(0x00FF00, MesfColorTable.colorFor(2))
        assertEquals(0x0000FF, MesfColorTable.colorFor(3))
    }

    @Test
    fun derivedColorsFollowBlendTable() {
        val gb = blend(MesfColorTable.colorFor(2), MesfColorTable.colorFor(3))
        val rb = blend(MesfColorTable.colorFor(1), MesfColorTable.colorFor(3))
        val rg = blend(MesfColorTable.colorFor(1), MesfColorTable.colorFor(2))

        assertEquals(blend(MesfColorTable.colorFor(1), gb), MesfColorTable.colorFor(4))
        assertEquals(blend(MesfColorTable.colorFor(2), rb), MesfColorTable.colorFor(5))
        assertEquals(blend(MesfColorTable.colorFor(3), rg), MesfColorTable.colorFor(6))
        assertEquals(blend(MesfColorTable.colorFor(3), gb), MesfColorTable.colorFor(7))
        assertEquals(blend(MesfColorTable.colorFor(2), rg), MesfColorTable.colorFor(8))
        assertEquals(blend(MesfColorTable.colorFor(1), rb), MesfColorTable.colorFor(9))

        assertEquals(blend(MesfColorTable.colorFor(4), MesfColorTable.colorFor(5)), MesfColorTable.colorFor(10))
        assertEquals(blend(MesfColorTable.colorFor(4), MesfColorTable.colorFor(6)), MesfColorTable.colorFor(11))
        assertEquals(blend(MesfColorTable.colorFor(5), MesfColorTable.colorFor(6)), MesfColorTable.colorFor(12))
        assertEquals(blend(MesfColorTable.colorFor(7), MesfColorTable.colorFor(8)), MesfColorTable.colorFor(13))
        assertEquals(blend(MesfColorTable.colorFor(7), MesfColorTable.colorFor(9)), MesfColorTable.colorFor(14))
        assertEquals(blend(MesfColorTable.colorFor(8), MesfColorTable.colorFor(9)), MesfColorTable.colorFor(15))

        assertEquals(blend(MesfColorTable.colorFor(10), MesfColorTable.colorFor(13)), MesfColorTable.colorFor(16))
        assertEquals(blend(MesfColorTable.colorFor(11), MesfColorTable.colorFor(14)), MesfColorTable.colorFor(17))
        assertEquals(blend(MesfColorTable.colorFor(12), MesfColorTable.colorFor(15)), MesfColorTable.colorFor(18))
        assertEquals(blend(MesfColorTable.colorFor(10), MesfColorTable.colorFor(15)), MesfColorTable.colorFor(19))
        assertEquals(blend(MesfColorTable.colorFor(12), MesfColorTable.colorFor(13)), MesfColorTable.colorFor(20))

        assertEquals(blend(MesfColorTable.colorFor(16), MesfColorTable.colorFor(17)), MesfColorTable.colorFor(21))
        assertEquals(blend(MesfColorTable.colorFor(17), MesfColorTable.colorFor(18)), MesfColorTable.colorFor(22))
        assertEquals(blend(MesfColorTable.colorFor(18), MesfColorTable.colorFor(19)), MesfColorTable.colorFor(23))
        assertEquals(blend(MesfColorTable.colorFor(19), MesfColorTable.colorFor(20)), MesfColorTable.colorFor(24))
        assertEquals(blend(MesfColorTable.colorFor(16), MesfColorTable.colorFor(20)), MesfColorTable.colorFor(25))
        assertEquals(blend(MesfColorTable.colorFor(16), MesfColorTable.colorFor(18)), MesfColorTable.colorFor(26))
    }

    // Independent re-implementation of the averaging blend, so this test
    // doesn't just call back into the code under test.
    private fun blend(c1: Int, c2: Int): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val r = (r1 + r2) / 2
        val g = (g1 + g2) / 2
        val b = (b1 + b2) / 2
        return (r shl 16) or (g shl 8) or b
    }
}
