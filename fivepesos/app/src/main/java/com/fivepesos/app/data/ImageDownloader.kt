package com.fivepesos.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads [imageUrl] -- typically an `<img src>` hit from the in-app
 * Google search WebView -- decodes it, and saves it as a PNG under the
 * app's private files dir as [fileName]. Returns a `file://` Uri our own
 * ContentResolver can always read back (no persistable-permission dance
 * needed, we own the file), or null on any failure: bad host, a non-image
 * response, a timeout, a site that blocks hotlinking, etc.
 */
suspend fun downloadImageToFile(context: Context, imageUrl: String, fileName: String): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val bitmap: Bitmap? = connection.inputStream.use { BitmapFactory.decodeStream(it) }
            connection.disconnect()
            if (bitmap == null) return@runCatching null
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            Uri.fromFile(file)
        }.getOrNull()
    }
