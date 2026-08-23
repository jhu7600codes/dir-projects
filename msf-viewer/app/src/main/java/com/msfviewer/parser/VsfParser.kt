package com.msfviewer.parser

/**
 * Turns a `.vsf` filename into a sequence of MESF frames -- VESF ("Video
 * Encoded via Seed") is exactly the MESF image format, except '-' now
 * means "go forward a frame" instead of being an ordinary punctuation
 * character.
 *
 * A design choice worth calling out, since the task describing this
 * format didn't spell it out: here, '-' is a *pure* frame separator, not
 * punctuation that also happens to advance a frame. It's consumed by
 * splitting the name on it before any per-character parsing happens, so
 * it never reaches [MesfParser.tokenize] and never triggers MESF's
 * punctuation size-boost behavior (no bumping the unit before/after it,
 * no orphan transparent unit). Everything else about a frame -- letters,
 * digits, spaces, and every *other* punctuation mark -- is tokenized by
 * the exact same rules as a `.msf` file, run independently per frame
 * (so e.g. a repeat-count digit or a punctuation boost never crosses a
 * '-' boundary into a neighboring frame).
 *
 * The easter egg is checked once against the whole name (before
 * splitting), matching how `.msf`'s "fish" check works -- not per frame.
 */
object VsfParser {

    private const val EXTENSION = ".vsf"

    /** Strips a trailing ".vsf" (case-insensitive), if present. */
    fun stripExtension(filename: String): String {
        return if (filename.length >= EXTENSION.length &&
            filename.regionMatches(
                filename.length - EXTENSION.length,
                EXTENSION,
                0,
                EXTENSION.length,
                ignoreCase = true,
            )
        ) {
            filename.substring(0, filename.length - EXTENSION.length)
        } else {
            filename
        }
    }

    fun parse(filename: String): VsfParseResult {
        val baseName = stripExtension(filename)

        if (baseName.trim().equals("fish", ignoreCase = true)) {
            return VsfParseResult(baseName = baseName, isEasterEgg = true, frames = emptyList())
        }

        // A leading/trailing/doubled '-' produces an empty segment, which
        // tokenizes to zero units -- a legitimate blank frame (nothing
        // drawn for its duration), not an error, so segments are never
        // filtered out here.
        val frames = baseName.split('-').map { segment -> MesfParser.tokenize(segment) }

        return VsfParseResult(baseName = baseName, isEasterEgg = false, frames = frames)
    }
}
