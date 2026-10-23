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
}
