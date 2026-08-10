package com.maytube.app.download

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises SabrFragmentParser against hand-built buffers matching the exact
 * wire format from back/backend.js's /sabr_playback response, cross-checked
 * against the reference client parser in
 * assets/site-assets/html5-player.js (sabrProcessParts).
 */
class SabrFragmentParserTest {

    private fun buildPart(id: String, data: ByteArray): ByteArray {
        val header = "//SPART-\"$id\"-CL=${data.size}//"
        return header.toByteArray(Charsets.US_ASCII) + data
    }

    private fun buildResponse(vararg parts: Pair<String, ByteArray>): ByteArray {
        var body = "SABER-START///".toByteArray(Charsets.US_ASCII)
        for ((id, data) in parts) {
            body += buildPart(id, data)
        }
        return body
    }

    @Test
    fun `parses a single video part`() {
        val videoBytes = byteArrayOf(1, 2, 3, 4, 5)
        val response = buildResponse("136-40" to videoBytes)

        val parts = SabrFragmentParser.parse(response, 1)

        assertEquals(1, parts.size)
        assertEquals(136, parts[0].itag)
        assertEquals(40, parts[0].chunkNumber)
        assertFalse(parts[0].isAudio)
        assertArrayEquals(videoBytes, parts[0].data)
    }

    @Test
    fun `parses interleaved video and audio parts in order`() {
        val video = ByteArray(4096) { (it % 251).toByte() }
        val audio = ByteArray(2048) { ((it * 7) % 251).toByte() }
        val response = buildResponse("136-40" to video, "140-40" to audio)

        val parts = SabrFragmentParser.parse(response, 2)

        assertEquals(2, parts.size)
        assertFalse(parts[0].isAudio)
        assertArrayEquals(video, parts[0].data)
        assertTrue(parts[1].isAudio)
        assertArrayEquals(audio, parts[1].data)
    }

    @Test
    fun `classifies both known audio itags correctly`() {
        val response = buildResponse(
            "139-1" to byteArrayOf(9),
            "140-1" to byteArrayOf(9),
            "298-1" to byteArrayOf(9)
        )

        val parts = SabrFragmentParser.parse(response, 3)

        assertTrue(parts[0].isAudio)
        assertTrue(parts[1].isAudio)
        assertFalse(parts[2].isAudio)
    }

    @Test
    fun `handles binary payloads that happen to contain slash-slash bytes`() {
        // the payload intentionally embeds a literal "//" so the parser must
        // rely on the declared content-length, not on scanning for the next
        // "//" to find the end of the data section
        val tricky = byteArrayOf('/'.code.toByte(), '/'.code.toByte(), 0, 1, 2, '/'.code.toByte())
        val response = buildResponse("134-7" to tricky)

        val parts = SabrFragmentParser.parse(response, 1)

        assertEquals(1, parts.size)
        assertArrayEquals(tricky, parts[0].data)
    }

    @Test
    fun `returns empty list when part count is zero`() {
        val response = "SABER-START///".toByteArray(Charsets.US_ASCII)
        assertTrue(SabrFragmentParser.parse(response, 0).isEmpty())
    }

    @Test(expected = SabrFragmentParser.MalformedResponseException::class)
    fun `rejects a response missing the SABER-START prefix`() {
        SabrFragmentParser.parse("not a sabr response".toByteArray(), 1)
    }

    @Test(expected = SabrFragmentParser.MalformedResponseException::class)
    fun `rejects a truncated response`() {
        val response = buildResponse("136-1" to ByteArray(100))
        SabrFragmentParser.parse(response.copyOf(response.size - 50), 1)
    }
}
