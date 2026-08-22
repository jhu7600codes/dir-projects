package com.msfviewer.parser

/**
 * Turns a `.msf` filename into the list of [MesfUnit]s the spec describes.
 *
 * A few points in the spec are stated generally rather than exhaustively;
 * the choices below are noted where they matter, so the behavior is
 * traceable back to the spec text rather than silently invented:
 *
 * - "a digit appearing directly after a letter is a repeat count for that
 *   letter" is read as: the digit multiplies *everything that single
 *   letter produced* (so an uppercase letter's whole digit-split group
 *   repeats together, not just its last digit) -- this is the direct
 *   generalization of the given "a2b" example, which only shows a
 *   single-unit letter.
 * - A repeat count of 0 legitimately produces zero units for that letter
 *   -- literal reading of "repeat count", not special-cased away.
 * - Punctuation's "+2 to the unit immediately before/after" targets the
 *   nearest actual emitted unit in the output sequence, which includes
 *   transparent space-units (the spec explicitly calls a space "a unit").
 *   The documented fallback ("no letter/number on either side" -> the
 *   punctuation becomes its own bigger transparent unit) is reachable
 *   exactly when there is no unit anywhere before or after it in the
 *   whole name -- i.e. the name is punctuation-only.
 */
object MesfParser {

    private const val EXTENSION = ".msf"

    /** Strips a trailing ".msf" (case-insensitive), if present. */
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

    fun parse(filename: String): MesfParseResult {
        val baseName = stripExtension(filename)

        if (baseName.equals("fish", ignoreCase = true)) {
            return MesfParseResult(baseName = baseName, isEasterEgg = true, units = emptyList())
        }

        return MesfParseResult(baseName = baseName, isEasterEgg = false, units = tokenize(baseName))
    }

    private fun digitsOf(value: Int): List<Int> =
        if (value < 10) listOf(value) else value.toString().map { it - '0' }

    private fun tokenize(name: String): List<MesfUnit> {
        val units = mutableListOf<MesfUnit>()

        // Punctuation marks seen so far that are still waiting for the
        // *next* unit to appear, to bump it (+2 each). Drained the moment
        // any unit is appended.
        var pendingAfterBoosts = 0

        // Punctuation marks that, at the moment they were seen, had no
        // unit before them either. If a later unit ever appears, they're
        // resolved via pendingAfterBoosts (addUnit clears both together).
        // Whatever is left here at the end of the string had no unit on
        // either side -- the name was punctuation-only from that point.
        var orphanPunctCount = 0

        fun addUnit(colorNumber: Int?) {
            val unit = MesfUnit(colorNumber = colorNumber)
            if (pendingAfterBoosts > 0) {
                unit.size += 2 * pendingAfterBoosts
                pendingAfterBoosts = 0
            }
            orphanPunctCount = 0
            units.add(unit)
        }

        // Emits a letter's color(s), applying a following digit (if any)
        // as a repeat count for the whole group. Returns the next index.
        fun processLetter(colorDigits: List<Int>, index: Int): Int {
            var next = index + 1
            val repeatCount = if (next < name.length && name[next] in '0'..'9') {
                val count = name[next] - '0'
                next += 1
                count
            } else {
                1
            }
            repeat(repeatCount) { colorDigits.forEach(::addUnit) }
            return next
        }

        var i = 0
        while (i < name.length) {
            val ch = name[i]
            i = when {
                ch in 'a'..'z' -> processLetter(listOf(ch - 'a' + 1), i)
                ch in 'A'..'Z' -> processLetter(digitsOf(ch - 'A' + 1), i)
                ch in '0'..'9' -> {
                    // Standalone digit -- not consumed as a repeat count
                    // above, since processLetter already skips past those.
                    addUnit(ch - '0')
                    i + 1
                }
                ch == ' ' -> {
                    addUnit(null)
                    i + 1
                }
                else -> {
                    // Punctuation: not a unit itself.
                    if (units.isNotEmpty()) {
                        units.last().size += 2
                    } else {
                        orphanPunctCount += 1
                    }
                    pendingAfterBoosts += 1
                    i + 1
                }
            }
        }

        // Punctuation with no unit on either side becomes its own bigger
        // transparent unit (base size 1, +2 since it's its own neighbor).
        repeat(orphanPunctCount) {
            units.add(MesfUnit(colorNumber = null, size = 3))
        }

        return units
    }
}
