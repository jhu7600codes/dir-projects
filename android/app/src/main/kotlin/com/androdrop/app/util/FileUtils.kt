package com.androdrop.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

data class SharedFileInfo(val name: String, val size: Long, val mimeType: String)

fun resolveFileInfo(context: Context, uri: Uri): SharedFileInfo {
    var name = "file"
    var size = -1L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
        }
    }
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    return SharedFileInfo(name, size, mimeType)
}

/** Streams a content:// Uri's bytes into the multipart body without loading the whole file into memory. */
fun uriMultipartPart(context: Context, uri: Uri, partName: String = "files"): MultipartBody.Part {
    val info = resolveFileInfo(context, uri)
    val resolver = context.contentResolver
    val body = object : RequestBody() {
        override fun contentType() = info.mimeType.toMediaTypeOrNull()
        override fun contentLength() = if (info.size >= 0) info.size else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(uri)?.use { input ->
                sink.writeAll(input.source())
            }
        }
    }
    return MultipartBody.Part.createFormData(partName, info.name, body)
}
