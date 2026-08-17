package com.androdrop.app.util

import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

/** Kotlin port of the web app's src/lib/format-bytes.ts — keep in sync. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val i = min((ln(bytes.toDouble()) / ln(1024.0)).toInt(), units.size - 1)
    val value = bytes / 1024.0.pow(i)
    return if (i == 0) "${value.toInt()} ${units[i]}" else "%.1f %s".format(value, units[i])
}
