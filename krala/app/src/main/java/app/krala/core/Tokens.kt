package app.krala.core

/**
 * Cheap token estimate, good enough for budgeting. Latin text averages ~4 chars per token,
 * cyrillic and cjk are denser, so non-ascii chars count more.
 */
object Tokens {
    fun estimate(text: String): Int {
        if (text.isEmpty()) return 0
        var ascii = 0
        var other = 0
        for (c in text) if (c.code < 128) ascii++ else other++
        return (ascii / 4.0 + other / 2.5).toInt() + 1
    }

    /** images are billed differently everywhere, ~800 tokens is a fair middle estimate */
    const val IMAGE_TOKENS = 800

    /** per message overhead (role, separators) */
    const val MESSAGE_OVERHEAD = 4
}
