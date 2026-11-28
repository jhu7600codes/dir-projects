package app.krala.data.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import app.krala.core.Ids
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Images attached to messages: downscaled jpegs in files/attachments, sent as data urls. */
@Singleton
class AttachmentStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir = File(context.filesDir, "attachments").apply { mkdirs() }
    private val ser = ListSerializer(String.serializer())

    fun file(name: String) = File(dir, name)

    fun decode(json: String): List<String> = runCatching { Json.decodeFromString(ser, json) }.getOrDefault(emptyList())
    fun encode(names: List<String>): String = Json.encodeToString(ser, names)

    /** image file names in an attachments json (documents are stored as "doc:" entries) */
    fun images(json: String): List<String> = decode(json).filterNot { it.startsWith(Documents.DOC_PREFIX) }
    fun docs(json: String): List<AttachedDoc> = decode(json).mapNotNull { Documents.decode(it) }

    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bmp = load(uri) ?: return@withContext null
            val name = Ids.new() + ".jpg"
            file(name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()
            name
        } catch (e: Exception) {
            android.util.Log.w("krala", "attachment import failed", e)
            null
        }
    }

    private fun load(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = MAX_SIDE.toFloat() / max(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = (MAX_SIDE.toFloat() / max(raw.width, raw.height)).coerceAtMost(1f)
        if (rotation == 0f && scale == 1f) return raw
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it != raw) raw.recycle() }
    }

    fun dataUrl(name: String): String? = runCatching {
        "data:image/jpeg;base64," + Base64.encodeToString(file(name).readBytes(), Base64.NO_WRAP)
    }.getOrNull()

    fun delete(name: String) {
        file(name).delete()
    }

    companion object {
        const val MAX_SIDE = 1568
    }
}
