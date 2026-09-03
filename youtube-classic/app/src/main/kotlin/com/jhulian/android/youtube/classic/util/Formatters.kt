package com.jhulian.android.youtube.classic.util

import java.util.Locale
import kotlin.math.roundToLong

/** Small formatting helpers matching how the 2019 YouTube app rendered numbers. */
object Formatters {

    fun viewCount(views: Long): String {
        if (views < 0) return ""
        val compact = compactNumber(views)
        return "$compact views"
    }

    fun compactCount(count: Long): String = compactNumber(count)

    private fun compactNumber(value: Long): String {
        if (value < 1_000) return value.toString()
        val units = listOf("K" to 1_000L, "M" to 1_000_000L, "B" to 1_000_000_000L)
        var chosenSuffix = ""
        var chosenDivisor = 1L
        for ((suffix, divisor) in units) {
            if (value >= divisor) {
                chosenSuffix = suffix
                chosenDivisor = divisor
            }
        }
        val scaled = value.toDouble() / chosenDivisor
        val rounded = (scaled * 10).roundToLong() / 10.0
        val text = if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
        return "$text$chosenSuffix"
    }

    fun duration(seconds: Long): String {
        if (seconds < 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    /** Relative time from a upload-date epoch (seconds), e.g. "3 days ago". */
    fun relativeTime(epochSeconds: Long): String {
        if (epochSeconds <= 0) return ""
        val now = System.currentTimeMillis() / 1000
        var diff = now - epochSeconds
        if (diff < 0) diff = 0

        val minute = 60L
        val hour = 60 * minute
        val day = 24 * hour
        val month = 30 * day
        val year = 365 * day

        return when {
            diff < minute -> "just now"
            diff < hour -> plural(diff / minute, "minute")
            diff < day -> plural(diff / hour, "hour")
            diff < month -> plural(diff / day, "day")
            diff < year -> plural(diff / month, "month")
            else -> plural(diff / year, "year")
        }
    }

    private fun plural(amount: Long, unit: String): String {
        val safeAmount = if (amount <= 0) 1 else amount
        return "$safeAmount $unit${if (safeAmount == 1L) "" else "s"} ago"
    }
}
