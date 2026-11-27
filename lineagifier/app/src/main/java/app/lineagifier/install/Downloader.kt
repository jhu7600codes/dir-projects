package app.lineagifier.install

import app.lineagifier.catalog.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class HashMismatch(val expected: String, val actual: String) :
    IOException("sha256 mismatch: expected $expected, got $actual")

object Downloader {
    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { s ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * downloads to [dest], hashing as it goes. tries the main url, then the archive fallback
     * (f-droid moves older versions there). deletes the file on any failure, including a bad hash.
     */
    suspend fun fetch(d: Download, dest: File, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        // reuse a previous download only if it still hashes right
        if (dest.exists() && dest.length() == d.size && sha256(dest).equals(d.sha256, true)) {
            progress(1f); return@withContext dest
        }
        val urls = listOfNotNull(d.url, d.fallbackUrl)
        var last: Throwable? = null
        for (u in urls) {
            try {
                fetchOne(u, d, dest, progress)
                return@withContext dest
            } catch (h: HashMismatch) {
                dest.delete(); throw h // a wrong hash from the real host is not something to retry around
            } catch (t: Throwable) {
                dest.delete(); last = t
                coroutineContext.ensureActive()
            }
        }
        throw IOException("download failed (${urls.joinToString()}): ${last?.message}", last)
    }

    private suspend fun fetchOne(url: String, d: Download, dest: File, progress: (Float) -> Unit) {
        require(url.startsWith("https://")) { "refusing non-https url $url" }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            val code = conn.responseCode
            if (code != 200) throw IOException("http $code for $url")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: d.size
            val md = MessageDigest.getInstance("SHA-256")
            dest.parentFile?.mkdirs()
            var done = 0L
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n); md.update(buf, 0, n)
                        done += n
                        progress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            val actual = md.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(d.sha256, ignoreCase = true)) throw HashMismatch(d.sha256, actual)
        } finally {
            conn.disconnect()
        }
    }
}
