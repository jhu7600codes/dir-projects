package com.vaultgame.core.world

/**
 * Visual/rooftop-type variety for a segment. [unlockDistance] is the minimum run distance
 * (meters) at which the generator starts rolling this theme, so the path opens up with more
 * variety the further a run goes rather than showing everything from meter zero.
 */
enum class SegmentTheme(val unlockDistance: Double) {
    RESIDENTIAL_ROWHOUSE(0.0),
    WAREHOUSE_DISTRICT(0.0),
    CONSTRUCTION_SITE(400.0),
    NEON_DOWNTOWN(900.0),
    BILLBOARD_PLAZA(1600.0),
    RAIL_YARD_OVERPASS(2500.0),
    SKYLINE_PENTHOUSE(4000.0);

    companion object {
        fun availableAt(distance: Double): List<SegmentTheme> =
            entries.filter { distance >= it.unlockDistance }
    }
}
