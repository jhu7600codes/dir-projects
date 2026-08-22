package com.msfviewer.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Resolves the actual filename a [Uri] points at.
 *
 * Most file managers / share sheets hand over a `content://` Uri, not a
 * raw file path -- the real filename has to come from the content
 * provider's DISPLAY_NAME column, not the Uri itself (its "path" segment
 * is often an opaque document id). `file://` Uris are handled directly
 * from their last path segment. Either way this never opens or reads the
 * file's bytes -- the whole point of the format is that the image comes
 * from the filename alone.
 */
object FileNameResolver {

    fun resolve(context: Context, uri: Uri): String? {
        return when (uri.scheme?.lowercase()) {
            "content" -> resolveContentDisplayName(context.contentResolver, uri) ?: uri.lastPathSegment
            "file" -> uri.path?.let { File(it).name }
            else -> uri.lastPathSegment
        }
    }

    private fun resolveContentDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            // A misbehaving/unavailable provider shouldn't crash the app --
            // fall back to the Uri's last path segment instead.
            null
        }
    }
}
