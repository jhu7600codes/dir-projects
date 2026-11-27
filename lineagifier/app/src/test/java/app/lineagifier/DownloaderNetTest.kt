package app.lineagifier

import app.lineagifier.catalog.Catalog
import app.lineagifier.install.Downloader
import app.lineagifier.install.HashMismatch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** hits f-droid for real. run with LINEAGIFIER_NET_TEST=1 ./gradlew testDebugUnitTest */
class DownloaderNetTest {
    private val cat = Catalog.parse(File("src/main/assets/catalog.json").readText())
    private val small = cat.entries.flatMap { it.installables() }.minBy { it.download.size }
    private val dir = Files.createTempDirectory("dl").toFile()

    @Before fun onlyWhenAsked() = assumeTrue(System.getenv("LINEAGIFIER_NET_TEST") == "1")

    @Test fun downloadsAndVerifiesWithProgress() = runBlocking {
        var last = 0f
        val f = Downloader.fetch(small.download, File(dir, "a.apk")) { last = it }
        assertEquals(small.download.size, f.length())
        assertEquals(small.download.sha256, Downloader.sha256(f))
        assertEquals(1f, last, 0.001f)
    }

    @Test fun wrongHashDeletesFile() = runBlocking {
        val dest = File(dir, "b.apk")
        try {
            Downloader.fetch(small.download.copy(sha256 = "0".repeat(64)), dest) {}
            fail("should have thrown")
        } catch (e: HashMismatch) {
            assertEquals(small.download.sha256, e.actual)
        }
        assertFalse(dest.exists())
    }

    @Test fun fallsBackToSecondUrl() = runBlocking {
        val d = small.download.copy(url = "https://f-droid.org/repo/lineagifier-does-not-exist.apk", fallbackUrl = small.download.url)
        val f = Downloader.fetch(d, File(dir, "c.apk")) {}
        assertEquals(small.download.sha256, Downloader.sha256(f))
    }
}
