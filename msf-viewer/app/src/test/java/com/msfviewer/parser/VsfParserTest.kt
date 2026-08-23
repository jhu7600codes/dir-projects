package com.msfviewer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VsfParserTest {

    private fun frameColors(frames: List<List<MesfUnit>>) = frames.map { frame -> frame.map { it.colorNumber } }

    @Test
    fun stripsVsfExtensionCaseInsensitively() {
        assertEquals("hello", VsfParser.stripExtension("hello.vsf"))
        assertEquals("hello", VsfParser.stripExtension("hello.VSF"))
        assertEquals("hello", VsfParser.stripExtension("hello.Vsf"))
        assertEquals("hello.msf", VsfParser.stripExtension("hello.msf"))
    }

    @Test
    fun dashSplitsIntoFrames() {
        // a-b-c -> three one-unit frames: [1], [2], [3]
        val result = VsfParser.parse("a-b-c.vsf")
        assertEquals(3, result.frames.size)
        assertEquals(listOf(listOf(1), listOf(2), listOf(3)), frameColors(result.frames))
    }

    @Test
    fun eachFrameUsesTheSameMesfTokenizationRulesAsAStandaloneMsfName() {
        // abc-J2 -- first frame is exactly what "abc.msf" would produce;
        // second frame is exactly what "J2.msf" would produce (uppercase
        // doesn't consume the following digit as a repeat count).
        val result = VsfParser.parse("abc-J2.vsf")
        val expectedFrame0 = MesfParser.parse("abc.msf").units.map { it.colorNumber }
        val expectedFrame1 = MesfParser.parse("J2.msf").units.map { it.colorNumber }
        assertEquals(listOf(expectedFrame0, expectedFrame1), frameColors(result.frames))
    }

    @Test
    fun dashIsAPureSeparatorWithNoPunctuationBoostEffect() {
        // In a.b (a plain MSF-style name) the dot bumps both 'a' and 'b'.
        // A dash between two letters in a VSF name, though, is consumed
        // entirely by the frame split -- it never reaches the tokenizer,
        // so neither 'a' nor 'b' gets sized up the way punctuation would.
        val result = VsfParser.parse("a-b.vsf")
        assertEquals(2, result.frames.size)
        assertEquals(1, result.frames[0].size)
        assertEquals(1, result.frames[0][0].size)
        assertEquals(1, result.frames[1].size)
        assertEquals(1, result.frames[1][0].size)
    }

    @Test
    fun leadingTrailingAndDoubledDashesProduceEmptyFrames() {
        val result = VsfParser.parse("-a--.vsf")
        // "" , "a", "", "" -> four frames, three of them empty
        assertEquals(4, result.frames.size)
        assertTrue(result.frames[0].isEmpty())
        assertEquals(listOf(1), result.frames[1].map { it.colorNumber })
        assertTrue(result.frames[2].isEmpty())
        assertTrue(result.frames[3].isEmpty())
    }

    @Test
    fun noDashesIsASingleFrame() {
        val result = VsfParser.parse("abc.vsf")
        assertEquals(1, result.frames.size)
        assertEquals(listOf(1, 2, 3), result.frames[0].map { it.colorNumber })
    }

    @Test
    fun fishTriggersEasterEggOnTheWholeNameBeforeSplitting() {
        val result = VsfParser.parse("fish.vsf")
        assertTrue(result.isEasterEgg)
        assertTrue(result.frames.isEmpty())

        // "fi-sh" is NOT "fish" as a whole (it's two frame segments), so
        // it does not trigger the easter egg -- the check runs on the
        // full name before any splitting.
        assertFalse(VsfParser.parse("fi-sh.vsf").isEasterEgg)
    }
}
