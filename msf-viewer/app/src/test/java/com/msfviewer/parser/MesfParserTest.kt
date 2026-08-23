package com.msfviewer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MesfParserTest {

    private fun colors(units: List<MesfUnit>) = units.map { it.colorNumber }
    private fun sizes(units: List<MesfUnit>) = units.map { it.size }

    @Test
    fun stripsMsfExtensionCaseInsensitively() {
        assertEquals("hello", MesfParser.stripExtension("hello.msf"))
        assertEquals("hello", MesfParser.stripExtension("hello.MSF"))
        assertEquals("hello", MesfParser.stripExtension("hello.Msf"))
        assertEquals("hello.txt", MesfParser.stripExtension("hello.txt"))
        assertEquals("hello", MesfParser.stripExtension("hello"))
    }

    @Test
    fun lowercaseAToI_mapDirectlyToColors1to9() {
        val result = MesfParser.parse("abcdefghi.msf")
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), colors(result.units))
        assertTrue(sizes(result.units).all { it == 1 })
    }

    @Test
    fun lowercaseJToZ_isOneUnitNotSplitIntoDigits() {
        val result = MesfParser.parse("j.msf")
        assertEquals(listOf(10), colors(result.units))
        assertEquals(1, result.units.size)

        val z = MesfParser.parse("z.msf")
        assertEquals(listOf(26), colors(z.units))
        assertEquals(1, z.units.size)
    }

    @Test
    fun uppercaseLetterSplitsIntoDigitsOfItsPosition() {
        // J is the 10th letter -> digits 1, 0 -> red, black
        val result = MesfParser.parse("J.msf")
        assertEquals(listOf(1, 0), colors(result.units))

        // A is the 1st letter -> single digit 1
        val a = MesfParser.parse("A.msf")
        assertEquals(listOf(1), colors(a.units))

        // Z is the 26th letter -> digits 2, 6
        val z = MesfParser.parse("Z.msf")
        assertEquals(listOf(2, 6), colors(z.units))
    }

    @Test
    fun digitAfterLetterIsRepeatCount() {
        // a2b -> a,a,b -> three units: color1, color1, color2
        val result = MesfParser.parse("a2b.msf")
        assertEquals(listOf(1, 1, 2), colors(result.units))
    }

    @Test
    fun uppercaseLetterDoesNotConsumeAFollowingDigitAsRepeatCount() {
        // J2 -> J's digits [1,0], then a standalone digit unit 2 -- the
        // repeat-count lookahead is lowercase-only.
        val result = MesfParser.parse("J2.msf")
        assertEquals(listOf(1, 0, 2), colors(result.units))
    }

    @Test
    fun repeatCountZeroStillProducesOneUnit() {
        // Repeat count is clamped to a minimum of 1, not 0.
        val result = MesfParser.parse("a0b.msf")
        assertEquals(listOf(1, 2), colors(result.units))
    }

    @Test
    fun standaloneDigitIsUsedDirectlyAsColorNumber() {
        val result = MesfParser.parse("5abc.msf")
        assertEquals(listOf(5, 1, 2, 3), colors(result.units))
    }

    @Test
    fun spaceIsATransparentUnit() {
        val result = MesfParser.parse("a b.msf")
        assertEquals(3, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertNull(result.units[1].colorNumber)
        assertEquals(2, result.units[2].colorNumber)
    }

    @Test
    fun punctuationBumpsSizeOfNeighborsOnBothSides() {
        // a.b -> '.' is not a unit; bumps 'a' and 'b' each by +2
        val result = MesfParser.parse("a.b.msf")
        assertEquals(2, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertEquals(3, result.units[0].size)
        assertEquals(2, result.units[1].colorNumber)
        assertEquals(3, result.units[1].size)
    }

    @Test
    fun consecutivePunctuationInsertsAnOrphanBetween() {
        // a..b -- the adjacency check only looks at the literal next
        // character. The first dot's next char is '.', not a letter/digit,
        // so it bumps 'a' AND spawns its own transparent unit; the second
        // dot's prev char is '.', not a letter/digit, so it doesn't bump
        // anything, but its next char 'b' is a letter, so it defers a
        // bump onto 'b'. Net: a(+2), an orphan transparent unit, b(+2).
        val result = MesfParser.parse("a..b.msf")
        assertEquals(3, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertEquals(3, result.units[0].size)
        assertNull(result.units[1].colorNumber)
        assertEquals(3, result.units[1].size)
        assertEquals(2, result.units[2].colorNumber)
        assertEquals(3, result.units[2].size)
    }

    @Test
    fun leadingPunctuationOnlyBumpsTheUnitAfterIt() {
        // Nothing precedes the dot, so its "before" side is a no-op; its
        // "after" side finds a letter and defers a bump onto it -- no
        // orphan unit gets created here since the following side succeeded.
        val result = MesfParser.parse(".a.msf")
        assertEquals(1, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertEquals(3, result.units[0].size)
    }

    @Test
    fun trailingPunctuationBumpsTheUnitBeforeItAndAlsoSpawnsAnOrphan() {
        // "a." doesn't end in ".msf" so nothing gets stripped here. The
        // dot's "before" side bumps 'a'; its "after" side has nothing (end
        // of string), which independently spawns its own transparent unit
        // -- the two effects aren't mutually exclusive.
        val result = MesfParser.parse("a.")
        assertEquals(2, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertEquals(3, result.units[0].size)
        assertNull(result.units[1].colorNumber)
        assertEquals(3, result.units[1].size)
    }

    @Test
    fun punctuationOnlyNameBecomesItsOwnBiggerTransparentUnits() {
        val result = MesfParser.parse("..")
        assertEquals(2, result.units.size)
        assertTrue(result.units.all { it.colorNumber == null && it.size == 3 })
    }

    @Test
    fun punctuationBetweenSpacesNeverBumpsTheSpacesAndBecomesItsOwnUnit() {
        // ' . ' -- a space is a unit, but not a "letter/number", and the
        // adjacency check is specifically letter/number. So neither
        // flanking space gets bumped; the dot becomes its own separate
        // transparent unit, size 3, in between two untouched size-1 spaces.
        val result = MesfParser.parse(" . ")
        assertEquals(3, result.units.size)
        assertTrue(result.units.all { it.colorNumber == null })
        assertEquals(1, result.units[0].size)
        assertEquals(3, result.units[1].size)
        assertEquals(1, result.units[2].size)
    }

    @Test
    fun punctuationBeforeUppercaseOnlyBumpsTheFirstDigitOfItsGroup() {
        // .J -- J splits into two units (1, 0); the deferred bump from the
        // dot lands only on the first of them.
        val result = MesfParser.parse(".J.msf")
        assertEquals(listOf(1, 0), colors(result.units))
        assertEquals(3, result.units[0].size)
        assertEquals(1, result.units[1].size)
    }

    @Test
    fun punctuationAfterUppercaseOnlyBumpsTheLastDigitOfItsGroup() {
        // J. -- the dot's "before" bump targets whatever was most
        // recently emitted, i.e. only the last of J's two digit units.
        val result = MesfParser.parse("J.")
        assertEquals(3, result.units.size)
        assertEquals(listOf(1, 0, null), colors(result.units))
        assertEquals(1, result.units[0].size)
        assertEquals(3, result.units[1].size)
        assertEquals(3, result.units[2].size) // the trailing orphan unit
    }

    @Test
    fun fishTriggersEasterEggCaseInsensitiveAndExtensionStripped() {
        assertTrue(MesfParser.parse("fish.msf").isEasterEgg)
        assertTrue(MesfParser.parse("FISH.msf").isEasterEgg)
        assertTrue(MesfParser.parse("FiSh.MSF").isEasterEgg)
        assertTrue(MesfParser.parse("fish").isEasterEgg)
        assertTrue(MesfParser.parse(" fish .msf").isEasterEgg) // surrounding whitespace is trimmed
        assertTrue(MesfParser.parse("fish.msf").units.isEmpty())
    }

    @Test
    fun nonFishNamesDoNotTriggerEasterEgg() {
        assertFalse(MesfParser.parse("fishy.msf").isEasterEgg)
        assertFalse(MesfParser.parse("goldfish.msf").isEasterEgg)
        assertFalse(MesfParser.parse("a.msf").isEasterEgg)
    }
}
