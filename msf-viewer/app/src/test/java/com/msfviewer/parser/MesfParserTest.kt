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
    fun repeatCountAppliesToWholeUppercaseDigitGroup() {
        // J2 -> J's digit group [1,0] repeated twice -> 1,0,1,0
        val result = MesfParser.parse("J2.msf")
        assertEquals(listOf(1, 0, 1, 0), colors(result.units))
    }

    @Test
    fun repeatCountZeroProducesNoUnitsForThatLetter() {
        val result = MesfParser.parse("a0b.msf")
        assertEquals(listOf(2), colors(result.units))
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
    fun consecutivePunctuationStacksBoosts() {
        // a..b -> two punctuation marks between a and b. Since nothing is
        // emitted between them, 'a' is "the unit immediately before" BOTH
        // dots, and 'b' is "the unit immediately after" BOTH dots -- so
        // each side stacks two +2 boosts (+4 total).
        val result = MesfParser.parse("a..b.msf")
        assertEquals(2, result.units.size)
        assertEquals(5, result.units[0].size)
        assertEquals(5, result.units[1].size)
    }

    @Test
    fun leadingPunctuationOnlyBumpsTheUnitAfterIt() {
        val result = MesfParser.parse(".a.msf")
        assertEquals(1, result.units.size)
        assertEquals(1, result.units[0].colorNumber)
        assertEquals(3, result.units[0].size)
    }

    @Test
    fun trailingPunctuationOnlyBumpsTheUnitBeforeIt() {
        // "a." doesn't end in ".msf" so nothing gets stripped here.
        val result = MesfParser.parse("a.")
        assertEquals(1, result.units.size)
        assertEquals(3, result.units[0].size)
    }

    @Test
    fun punctuationOnlyNameBecomesItsOwnBiggerTransparentUnits() {
        val result = MesfParser.parse("..")
        assertEquals(2, result.units.size)
        assertTrue(result.units.all { it.colorNumber == null && it.size == 3 })
    }

    @Test
    fun punctuationBetweenSpacesBumpsTheSpaces() {
        // ' . ' -- space, punctuation, space. The space units count as
        // neighbors, so both spaces get bumped rather than falling back.
        val result = MesfParser.parse(" . ")
        assertEquals(2, result.units.size)
        assertTrue(result.units.all { it.colorNumber == null })
        assertEquals(3, result.units[0].size)
        assertEquals(3, result.units[1].size)
    }

    @Test
    fun fishTriggersEasterEggCaseInsensitiveAndExtensionStripped() {
        assertTrue(MesfParser.parse("fish.msf").isEasterEgg)
        assertTrue(MesfParser.parse("FISH.msf").isEasterEgg)
        assertTrue(MesfParser.parse("FiSh.MSF").isEasterEgg)
        assertTrue(MesfParser.parse("fish").isEasterEgg)
        assertTrue(MesfParser.parse("fish.msf").units.isEmpty())
    }

    @Test
    fun nonFishNamesDoNotTriggerEasterEgg() {
        assertFalse(MesfParser.parse("fishy.msf").isEasterEgg)
        assertFalse(MesfParser.parse("goldfish.msf").isEasterEgg)
        assertFalse(MesfParser.parse("a.msf").isEasterEgg)
    }
}
