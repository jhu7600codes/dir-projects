package com.vanbank.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * VANBank stores every amount as whole minor units (kopecks -- 1 ruble = 100
 * kopecks) in `Long`, never as floating point, so running balances and
 * amortization schedules never drift from rounding error. This is the only
 * place that formats/parses between that and the ruble amounts a human
 * types or reads.
 */
object Money {
    const val MINOR_UNITS_PER_MAJOR = 100L
    private const val CURRENCY_SYMBOL = "₽" // ₽

    fun rublesToMinor(rubles: BigDecimal): Long =
        rubles.setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .toLong()

    fun minorToRubles(minor: Long): BigDecimal =
        BigDecimal(minor).movePointLeft(2)

    /** Parses user input like "1234.5" or "1,234.50" into minor units. Returns null if invalid. */
    fun parseToMinor(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace(CURRENCY_SYMBOL, "")
        if (cleaned.isEmpty()) return null
        val decimal = cleaned.toBigDecimalOrNull() ?: return null
        return rublesToMinor(decimal)
    }

    /** Formats minor units as "₽1,234.56" (always 2 decimals, thousands-grouped, no sign). */
    fun format(minor: Long): String {
        val negative = minor < 0
        val absMinor = kotlin.math.abs(minor)
        val whole = absMinor / MINOR_UNITS_PER_MAJOR
        val fraction = absMinor % MINOR_UNITS_PER_MAJOR
        val grouped = groupThousands(whole)
        val sign = if (negative) "-" else ""
        return "$sign$CURRENCY_SYMBOL$grouped.${fraction.toString().padStart(2, '0')}"
    }

    /** Formats with an explicit leading +/- sign, for transaction feeds. */
    fun formatSigned(minor: Long, direction: TransactionDirection): String {
        val prefix = if (direction == TransactionDirection.IN) "+" else "-"
        return prefix + format(kotlin.math.abs(minor))
    }

    private fun groupThousands(whole: Long): String {
        val s = whole.toString()
        val builder = StringBuilder()
        for ((index, char) in s.reversed().withIndex()) {
            if (index != 0 && index % 3 == 0) builder.append(',')
            builder.append(char)
        }
        return builder.reverse().toString()
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try {
            BigDecimal(this)
        } catch (e: NumberFormatException) {
            null
        }
}
