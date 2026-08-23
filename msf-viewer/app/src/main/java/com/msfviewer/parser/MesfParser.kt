package com.msfviewer.parser

/**
 * Turns a `.msf` filename into the list of [MesfUnit]s the spec describes.
 *
 * This is a direct Kotlin port of the reference MESF web renderer's exact
 * algorithm (not a from-scratch reading of the prose spec), since the
 * prose leaves some cases genuinely ambiguous and the reference resolves
 * them concretely. The behaviors worth calling out because they're easy
 * to get wrong by reasoning from the spec text alone:
 *
 * - Only a **lowercase** letter treats a following digit as a repeat
 *   count. A digit right after an **uppercase** letter is not consumed by
 *   it -- it's parsed on its own as a standalone color digit. ("J2" is
 *   red, black, green -- not red, black, red, black.)
 * - A repeat count of 0 still produces exactly one unit (a minimum of 1),
 *   not zero.
 * - Punctuation's size boost looks only at the *literal* adjacent
 *   characters, not the nearest unit transitively -- a space or another
 *   punctuation mark immediately next to it does not count as a
 *   "letter/number neighbor", even though a space is itself a unit. So a
 *   punctuation mark between two spaces bumps neither space; it becomes
 *   its own bigger transparent unit instead, and spaces are never resized.
 * - The "becomes its own bigger transparent unit" fallback is checked
 *   independently for the *following* side only: whenever the immediately
 *   next character isn't a letter/digit, the punctuation spawns its own
 *   transparent unit -- even if its *preceding* side did successfully
 *   bump a real unit. So trailing punctuation, or punctuation between two
 *   punctuation marks, produces an extra transparent unit in addition to
 *   any boost it gave the unit before it.
 * - When the following side does have a letter/digit neighbor, only the
 *   *first* unit that letter/digit produces gets the +2 (relevant for an
 *   uppercase letter, which can produce more than one unit).
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

        if (baseName.trim().equals("fish", ignoreCase = true)) {
            return MesfParseResult(baseName = baseName, isEasterEgg = true, units = emptyList())
        }

        return MesfParseResult(baseName = baseName, isEasterEgg = false, units = tokenize(baseName))
    }

    private fun digitsOf(value: Int): List<Int> =
        if (value < 10) listOf(value) else value.toString().map { it - '0' }

    private fun isLetterOrDigit(c: Char?): Boolean =
        c != null && (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9')

    /** A punctuation mark whose "bump the next unit" side hasn't resolved
     * yet -- the unit it targets doesn't exist until the following
     * character is processed. */
    private object PendingBoost

    /**
     * The character-by-character tokenizer on its own, with no extension
     * stripping or easter-egg check -- exposed (module-internal) so
     * [VsfParser] can run the identical per-character rules on each frame
     * of a `.vsf` name after it's already been split on '-'.
     */
    internal fun tokenize(name: String): List<MesfUnit> {
        // Mixed list of real units and PendingBoost placeholders, built in
        // one left-to-right pass exactly like the reference: a
        // placeholder always sits immediately before the real unit(s) its
        // owning punctuation mark is waiting to bump.
        val slots = mutableListOf<Any>() // MesfUnit | PendingBoost
        val n = name.length
        var i = 0

        while (i < n) {
            val ch = name[i]
            when {
                ch in 'a'..'z' -> {
                    val colorNumber = ch - 'a' + 1
                    var next = i + 1
                    var repeatCount = 1
                    if (next < n && name[next] in '0'..'9') {
                        repeatCount = name[next] - '0'
                        next += 1
                    }
                    // A repeat count of 0 still yields one unit (matches
                    // the reference's Math.max(repeat, 1) clamp).
                    kotlin.repeat(maxOf(repeatCount, 1)) {
                        slots.add(MesfUnit(colorNumber = colorNumber))
                    }
                    i = next
                }
                ch in 'A'..'Z' -> {
                    // Uppercase never consumes a following digit -- unlike
                    // lowercase, there's no repeat-count lookahead here.
                    val position = ch - 'A' + 1
                    digitsOf(position).forEach { digit ->
                        slots.add(MesfUnit(colorNumber = digit))
                    }
                    i += 1
                }
                ch in '0'..'9' -> {
                    slots.add(MesfUnit(colorNumber = ch - '0'))
                    i += 1
                }
                ch == ' ' -> {
                    slots.add(MesfUnit(colorNumber = null))
                    i += 1
                }
                else -> {
                    val prevCh = if (i > 0) name[i - 1] else null
                    if (isLetterOrDigit(prevCh)) {
                        // The most recently emitted slot is always a real
                        // unit here, never a still-pending placeholder: a
                        // placeholder is only ever created when the next
                        // character is a letter/digit, and that character
                        // gets processed on the very next loop iteration,
                        // immediately turning it into a real unit before
                        // any other punctuation mark could be reached.
                        (slots.lastOrNull() as? MesfUnit)?.let { it.size += 2 }
                    }
                    val nextCh = if (i + 1 < n) name[i + 1] else null
                    if (isLetterOrDigit(nextCh)) {
                        slots.add(PendingBoost)
                    } else {
                        // No letter/number on the following side -- becomes
                        // its own bigger transparent unit, regardless of
                        // whether the preceding side already got bumped.
                        slots.add(MesfUnit(colorNumber = null, size = 3))
                    }
                    i += 1
                }
            }
        }

        // Resolve placeholders: each one bumps the size of the first real
        // unit that appears after it (skipping over any other
        // placeholders in between, though by construction there never
        // are any -- a placeholder is always immediately followed by a
        // real unit on the next loop iteration above).
        val resolved = slots.filterIsInstance<MesfUnit>()
        for (k in slots.indices) {
            if (slots[k] !== PendingBoost) continue
            var targetIndex = k + 1
            while (targetIndex < slots.size && slots[targetIndex] === PendingBoost) targetIndex += 1
            if (targetIndex < slots.size) {
                val resolvedIndex = slots.subList(0, targetIndex).count { it is MesfUnit }
                resolved.getOrNull(resolvedIndex)?.let { it.size += 2 }
            }
        }

        return resolved
    }
}
