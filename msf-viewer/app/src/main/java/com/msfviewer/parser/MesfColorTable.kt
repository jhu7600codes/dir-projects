package com.msfviewer.parser

/**
 * The 27 colors (0-26) used by the MESF spec.
 *
 * Colors 0-3 are the fixed base colors (black/red/green/blue); colors 4-26
 * are derived by repeated pairwise averaging, per the fixed blend table in
 * the spec. `blend(x, y)` averages each RGB channel independently. The
 * whole table has a fixed dependency order (each derived color only
 * depends on colors defined earlier), so it's computed once here and
 * cached as a plain array -- no need to recompute per render.
 */
object MesfColorTable {

    /** color number (0-26) -> packed 0xRRGGBB */
    val colors: IntArray = buildColorTable()

    fun colorFor(number: Int): Int = colors[number]

    private fun rgb(r: Int, g: Int, b: Int): Int = (r shl 16) or (g shl 8) or b

    private fun blend(c1: Int, c2: Int): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        return rgb((r1 + r2) / 2, (g1 + g2) / 2, (b1 + b2) / 2)
    }

    private fun buildColorTable(): IntArray {
        val c = IntArray(27)
        c[0] = rgb(0, 0, 0)
        c[1] = rgb(255, 0, 0)
        c[2] = rgb(0, 255, 0)
        c[3] = rgb(0, 0, 255)

        val gb = blend(c[2], c[3])
        val rb = blend(c[1], c[3])
        val rg = blend(c[1], c[2])

        c[4] = blend(c[1], gb)
        c[7] = blend(c[3], gb)
        c[5] = blend(c[2], rb)
        c[8] = blend(c[2], rg)
        c[6] = blend(c[3], rg)
        c[9] = blend(c[1], rb)

        c[10] = blend(c[4], c[5])
        c[13] = blend(c[7], c[8])
        c[11] = blend(c[4], c[6])
        c[14] = blend(c[7], c[9])
        c[12] = blend(c[5], c[6])
        c[15] = blend(c[8], c[9])

        c[16] = blend(c[10], c[13])
        c[17] = blend(c[11], c[14])
        c[18] = blend(c[12], c[15])
        c[19] = blend(c[10], c[15])
        c[20] = blend(c[12], c[13])

        c[21] = blend(c[16], c[17])
        c[22] = blend(c[17], c[18])
        c[23] = blend(c[18], c[19])
        c[24] = blend(c[19], c[20])
        c[25] = blend(c[16], c[20])
        c[26] = blend(c[16], c[18])

        return c
    }
}
