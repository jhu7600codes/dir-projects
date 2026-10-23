package app.stash.providers

import app.stash.data.db.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {
    private fun yt(title: String, artist: String, secs: Long) =
        Track(id = title, title = title, artist = artist, durationMs = secs * 1000, provider = "youtube", sourceId = title)

    @Test
    fun picksTheRightUploadOverLiveAndCovers() {
        val want = Triple("Creep", "Radiohead", 238_000L)
        val good = yt("Creep", "Radiohead", 239)
        val live = yt("Creep (Live at Glastonbury 1997)", "Radiohead", 290)
        val cover = yt("Creep cover", "Some Guy", 236)
        val s = { t: Track -> Matcher.score(t, want.first, want.second, want.third) }
        assertTrue(s(good) >= Matcher.ACCEPT)
        assertTrue(s(good) > s(live))
        assertTrue(s(cover) < Matcher.ACCEPT)
    }

    @Test
    fun ignoresFeaturesAndAccents() {
        val t = yt("Beyonce - Halo", "BeyonceVEVO", 261)
        assertTrue(Matcher.score(t, "Halo (feat. Someone)", "Beyoncé", 261_000) >= Matcher.ACCEPT)
    }

    @Test
    fun splitsYoutubeTitles() {
        assertEquals("Daft Punk" to "Get Lucky", YtDlpProvider.splitArtistTitle("Daft Punk - Get Lucky (Official Audio)", "DaftPunkVEVO", false))
        assertEquals("Kino" to "Gruppa krovi", YtDlpProvider.splitArtistTitle("Gruppa krovi", "Kino - Topic", false))
        assertEquals("Uploader" to "Song", YtDlpProvider.splitArtistTitle("Song", "Uploader", true))
    }

    @Test
    fun cyrillicMatchesLatinSpelling() {
        // youtube music in english answers "Zemfira", spotify says "Земфира"
        val t = yt("ХОЧЕШЬ?", "Zemfira", 222)
        assertTrue(Matcher.score(t, "Хочешь?", "Земфира", 222_000) >= Matcher.ACCEPT)
        assertEquals("zemfira", Matcher.normalize("Земфира"))
    }

    @Test
    fun cyrillicBothSides() {
        val t = yt("ариведерчи", "Земфира", 165)
        assertTrue(Matcher.score(t, "Ариведерчи", "Земфира", 166_000) >= Matcher.ACCEPT)
    }

    @Test
    fun symbolOnlyTitleGoesByArtistAndLength() {
        val right = yt("Бесконечность", "Zemfira", 318)
        val wrong = yt("ПММЛ", "Zemfira", 212)
        assertTrue(Matcher.score(right, "∞", "Земфира", 316_000) >= Matcher.ACCEPT)
        assertTrue(Matcher.score(wrong, "∞", "Земфира", 316_000) < Matcher.ACCEPT)
        assertEquals("∞", Matcher.normalize("∞"))
    }

    /** a youtube music video row as the app parses it: split title, plus channel + raw title */
    private fun video(raw: String, channel: String, secs: Long): Pair<Track, String> {
        val (a, t) = YtDlpProvider.splitArtistTitle(raw, channel, false)
        return yt(t, a, secs).copy(id = raw) to "$channel $raw"
    }

    private fun score(v: Pair<Track, String>, title: String, artist: String, ms: Long, album: String? = null) =
        Matcher.score(v.first, title, artist, ms, album, v.second)

    @Test
    fun soundtrackUploadedByTheArtist() {
        // real result: "DOORS ORIGINAL SOUNDTRACK VOL. 1 - Elevator Jam" on the LSPLASH channel
        val v = video("DOORS ORIGINAL SOUNDTRACK VOL. 1 - Elevator Jam", "LSPLASH", 32)
        assertTrue(score(v, "Elevator Jam", "LSPLASH", 39_480, "Doors (Original Game Soundtrack), Vol. 1") >= Matcher.ACCEPT)
        val here = video("DOORS ORIGINAL SOUNDTRACK VOL. 1 - Here I Come", "LSPLASH", 112)
        assertTrue(score(here, "Here I Come", "LSPLASH", 112_000) >= Matcher.ACCEPT)
    }

    @Test
    fun soundtrackOnTheGamesChannelCountsTheAlbum() {
        val v = video("DOORS ORIGINAL SOUNDTRACK VOL. 6 - R4NS0M", "DOORS - Official Game Soundtrack", 84)
        val album = "Doors (Original Game Soundtrack), Vol. 6"
        assertTrue(score(v, "R4NS0M", "LSPLASH, jasper.creations", 83_732, album) >= Matcher.ACCEPT)
        // without the album it's just some channel with the same title
        assertTrue(score(v, "R4NS0M", "LSPLASH, jasper.creations", 83_732) < Matcher.ACCEPT)
    }

    @Test
    fun coversOfSoundtracksStillLose() {
        val cover = video("ROBLOX [ DOORS ] || Here I Come Metal Cover", "Red Skuller", 127)
        assertTrue(score(cover, "Here I Come", "LSPLASH", 112_000, "Doors (Original Game Soundtrack), Vol. 1") < Matcher.ACCEPT)
    }

    @Test
    fun anyCreditedArtistCounts() {
        val v = video("ghost party", "overtonight", 120)
        assertTrue(score(v, "ghost party", "someone else, overtonight", 114_437) >= Matcher.ACCEPT)
    }

    @Test
    fun kanaArtistMatchesRomaji() {
        assertEquals("takaokamizuki", Matcher.normalize("タカオカミズキ"))
        assertEquals("ochame 機能", Matcher.normalize("おちゃめ機能"))
        assertEquals("kyasane", Matcher.romanizeKana("きゃさね"))
        val v = video(
            "おちゃめ機能 / covered by 重音テトSV【SynthesizerV AI】Ochame Kinou （Fukkireta） / Kasane Teto",
            "takaokamizuki", 249,
        )
        assertTrue(score(v, "Ochame Kinou (feat. KASANE TETO) - Cover", "タカオカミズキ, Kasane Teto", 248_800) >= Matcher.ACCEPT)
    }

    @Test
    fun vocaloidMvWithJapaneseTitle() {
        val v = video("Giga - '劣等上等'(BRING IT ON) ft.鏡音リン・レン【MV】", "Giga", 236)
        assertTrue(score(v, "劣等上等 feat. 鏡音リン・レン", "Giga, 鏡音リン・レン", 235_096) >= Matcher.ACCEPT)
    }
}
