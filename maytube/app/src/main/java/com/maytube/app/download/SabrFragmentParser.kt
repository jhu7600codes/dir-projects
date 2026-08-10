package com.maytube.app.download

/**
 * Parses the custom framing yt2009's `/sabr_playback` endpoint uses
 * (back/backend.js, the "SABER-START///" response format) -- the exact
 * same bytes html5-player.js parses client-side to feed MediaSource
 * (assets/site-assets/html5-player.js, `sabrProcessParts`).
 *
 * Wire format:
 *   "SABER-START///"                                  (14 bytes, ASCII)
 *   then `x-part-count` repetitions of:
 *     `//SPART-"<itag>-<chunkNumber>"-CL=<length>//`   (ASCII header)
 *     <length> raw bytes                               (the fragment)
 *
 * Audio itags are 139/140 (audioItags in both yt2009sabr.js and
 * html5-player.js); everything else in this stream is video.
 */
object SabrFragmentParser {

    private const val HEADER_START = "SABER-START///"
    private val AUDIO_ITAGS = setOf(139, 140)

    data class Part(val itag: Int, val chunkNumber: Int, val data: ByteArray, val isAudio: Boolean)

    class MalformedResponseException(message: String) : Exception(message)

    fun parse(body: ByteArray, expectedPartCount: Int): List<Part> {
        if (expectedPartCount <= 0) return emptyList()

        val prefix = String(body, 0, minOf(HEADER_START.length, body.size), Charsets.US_ASCII)
        if (prefix != HEADER_START) {
            throw MalformedResponseException("response did not start with $HEADER_START")
        }

        val parts = ArrayList<Part>(expectedPartCount)
        var cursor = HEADER_START.length

        repeat(expectedPartCount) {
            // header is short ASCII text; peek a bounded window the same
            // way html5-player.js does (new Uint8Array(s.slice(cursor, cursor+70)))
            val peekEnd = minOf(cursor + 70, body.size)
            if (cursor >= body.size) {
                throw MalformedResponseException("ran out of bytes while reading part header")
            }
            val peek = String(body, cursor, peekEnd - cursor, Charsets.US_ASCII)

            // header looks like //SPART-"136-40"-CL=12345//...(binary)...
            val afterFirstSlashes = peek.indexOf("//", 0)
            if (afterFirstSlashes != 0) {
                throw MalformedResponseException("expected part header at cursor $cursor")
            }
            val closeIndex = peek.indexOf("//", 2)
            if (closeIndex == -1) {
                throw MalformedResponseException("could not find end of part header at cursor $cursor")
            }
            val headerText = peek.substring(2, closeIndex) // SPART-"136-40"-CL=12345
            // mirrors html5-player.js: headerLength = ("//" + partHeader + "//").length
            val headerLength = closeIndex + 2 // everything up to and including the closing "//"

            val partId = headerText.substringAfter("SPART-\"").substringBefore("\"")
            val length = headerText.substringAfter("-CL=").toIntOrNull()
                ?: throw MalformedResponseException("could not parse content length from '$headerText'")

            val dataStart = cursor + headerLength
            val dataEnd = dataStart + length
            if (dataEnd > body.size) {
                throw MalformedResponseException("part claims $length bytes but response is truncated")
            }

            val itag = partId.substringBefore("-").toIntOrNull()
                ?: throw MalformedResponseException("could not parse itag from part id '$partId'")
            val chunkNumber = partId.substringAfter("-").toIntOrNull() ?: 0

            parts.add(
                Part(
                    itag = itag,
                    chunkNumber = chunkNumber,
                    data = body.copyOfRange(dataStart, dataEnd),
                    isAudio = itag in AUDIO_ITAGS
                )
            )
            cursor = dataEnd
        }

        return parts
    }
}
