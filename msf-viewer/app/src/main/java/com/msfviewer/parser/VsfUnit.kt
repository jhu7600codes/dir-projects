package com.msfviewer.parser

/** Result of parsing a `.vsf` (VESF -- Video Encoded via Seed) filename. */
data class VsfParseResult(
    /** The filename with a trailing ".vsf" (case-insensitive) stripped. */
    val baseName: String,
    /** True if [baseName] is "fish" (case-insensitive) -- the easter egg,
     * checked against the whole name before it's split into frames. */
    val isEasterEgg: Boolean,
    /**
     * One entry per frame, in order. Each frame is the ordinary MESF unit
     * list for the text between two '-' characters (or the start/end of
     * the name), tokenized with the exact same per-character rules as a
     * `.msf` file. Empty when [isEasterEgg] is true.
     */
    val frames: List<List<MesfUnit>>,
)
