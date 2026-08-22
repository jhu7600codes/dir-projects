package com.msfviewer.parser

/**
 * One unit produced from the filename. [colorNumber] indexes
 * [MesfColorTable] (0-26); it's null for a transparent unit (a space, or
 * an orphaned punctuation mark with no letter/number on either side).
 * [size] is the unit's pixel size in multiples of the base unit size --
 * starts at 1, bumped by +2 per adjacent punctuation mark.
 */
data class MesfUnit(
    val colorNumber: Int?,
    var size: Int = 1,
)

/** Result of parsing a filename per the MESF spec. */
data class MesfParseResult(
    /** The filename with a trailing ".msf" (case-insensitive) stripped. */
    val baseName: String,
    /** True if [baseName] is "fish" (case-insensitive) -- the easter egg. */
    val isEasterEgg: Boolean,
    /** Empty when [isEasterEgg] is true -- the algorithm is skipped entirely. */
    val units: List<MesfUnit>,
)
