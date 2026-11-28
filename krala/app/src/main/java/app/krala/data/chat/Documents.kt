package app.krala.data.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.krala.core.DocText
import app.krala.core.Ids
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** an attached file: its text lives in files/attachments/<stored>, the name is what the user sees */
data class AttachedDoc(val stored: String, val name: String, val chars: Int)

/**
 * Files attached to messages. The text is pulled out once on import (pdf, docx, pptx, any plain
 * text or code) and stored next to the images; the model gets it as a --files-- section.
 */
@Singleton
class Documents @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir = File(context.filesDir, "attachments").apply { mkdirs() }
    private var pdfReady = false

    class Unsupported(val name: String) : Exception("can't read $name")

    suspend fun import(uri: Uri): AttachedDoc = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Unsupported(name)
        if (bytes.size > MAX_BYTES) throw Unsupported(name)
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = name.substringAfterLast('.', "").lowercase()
        val text = when {
            mime == "application/pdf" || ext == "pdf" -> pdf(bytes)
            ext == "docx" || mime.contains("wordprocessingml") -> zipText(bytes) { n -> n == "word/document.xml" }?.let(DocText::docxXml)
            ext == "pptx" || mime.contains("presentationml") -> zipSlides(bytes)
            DocText.looksLikeText(bytes) -> String(bytes, Charsets.UTF_8)
            else -> null
        }?.replace("\r\n", "\n")?.trim()
        if (text.isNullOrBlank()) throw Unsupported(name)
        val stored = Ids.new() + ".txt"
        File(dir, stored).writeText(text)
        AttachedDoc(stored, name, text.length)
    }

    fun read(stored: String): String? = runCatching { File(dir, stored).readText() }.getOrNull()

    fun delete(stored: String) {
        File(dir, stored).delete()
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"

    private fun pdf(bytes: ByteArray): String? {
        if (!pdfReady) { PDFBoxResourceLoader.init(context); pdfReady = true }
        return PDDocument.load(ByteArrayInputStream(bytes)).use { doc -> PDFTextStripper().getText(doc) }
    }

    private fun zipText(bytes: ByteArray, match: (String) -> Boolean): String? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (match(e.name)) return z.readBytes().toString(Charsets.UTF_8)
            }
        }
        return null
    }

    private fun zipSlides(bytes: ByteArray): String? {
        val slides = sortedMapOf<Int, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val n = Regex("""ppt/slides/slide(\d+)\.xml""").matchEntire(e.name)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                slides[n] = DocText.slideXml(z.readBytes().toString(Charsets.UTF_8))
            }
        }
        return slides.entries.joinToString("\n\n") { (i, t) -> "slide $i: $t" }.takeIf { slides.isNotEmpty() }
    }

    companion object {
        const val MAX_BYTES = 25 * 1024 * 1024
        const val DOC_PREFIX = "doc:"

        fun encode(d: AttachedDoc) = DOC_PREFIX + d.stored + "|" + d.chars + "|" + d.name
        fun decode(entry: String): AttachedDoc? {
            if (!entry.startsWith(DOC_PREFIX)) return null
            val parts = entry.removePrefix(DOC_PREFIX).split('|', limit = 3)
            if (parts.size < 3) return null
            return AttachedDoc(parts[0], parts[2], parts[1].toIntOrNull() ?: 0)
        }
    }
}
