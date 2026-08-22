package com.msfviewer.parser

/** A single unit placed in the flow layout, in base-unit coordinates. */
data class PlacedUnit(
    val unit: MesfUnit,
    val x: Int, // left edge, in base units
    val y: Int, // top edge, in base units
)

/** The full computed layout: every unit placed, plus overall dimensions. */
data class MesfLayout(
    val placedUnits: List<PlacedUnit>,
    val rowCount: Int,
    val widthUnits: Int, // overall image width, in base units
    val heightUnits: Int, // overall image height, in base units
)

/**
 * Lays out units in a simple left-to-right flow: each unit is placed at
 * its own (square) size, wrapping to a new row once the row width limit
 * is hit. Row height is the tallest unit placed in that row. Nothing is
 * ever cropped or dropped -- an oversized unit just gets a row to itself,
 * even if it alone exceeds the row width limit.
 */
object MesfLayoutEngine {

    /** Fixed row width limit, in base units -- there's no "correct" value
     * in the spec, just "pick a sensible fixed row width". */
    const val DEFAULT_ROW_WIDTH_UNITS = 16

    fun layout(units: List<MesfUnit>, rowWidthUnits: Int = DEFAULT_ROW_WIDTH_UNITS): MesfLayout {
        if (units.isEmpty()) {
            return MesfLayout(placedUnits = emptyList(), rowCount = 0, widthUnits = 0, heightUnits = 0)
        }

        val placed = mutableListOf<PlacedUnit>()
        var rowIndex = 0
        var cursorX = 0
        var rowY = 0
        var rowHeight = 0
        var maxWidth = 0

        for (unit in units) {
            if (cursorX > 0 && cursorX + unit.size > rowWidthUnits) {
                maxWidth = maxOf(maxWidth, cursorX)
                rowY += rowHeight
                rowIndex += 1
                cursorX = 0
                rowHeight = 0
            }
            placed.add(PlacedUnit(unit = unit, x = cursorX, y = rowY))
            cursorX += unit.size
            rowHeight = maxOf(rowHeight, unit.size)
        }
        maxWidth = maxOf(maxWidth, cursorX)
        val totalHeight = rowY + rowHeight

        return MesfLayout(
            placedUnits = placed,
            rowCount = rowIndex + 1,
            widthUnits = maxWidth,
            heightUnits = totalHeight,
        )
    }
}
