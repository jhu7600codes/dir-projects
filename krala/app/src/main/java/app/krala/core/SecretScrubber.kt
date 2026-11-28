package app.krala.core

/**
 * Removes things that must never end up in memory: api keys, tokens, passwords, card numbers.
 * Runs on every note before it is stored, and on recall excerpts.
 */
object SecretScrubber {
    const val MASK = "[secret removed]"

    private val patterns: List<Regex> = listOf(
        // provider style keys
        Regex("""\bsk-(?:proj-|ant-|or-v1-)?[A-Za-z0-9_\-]{16,}"""),
        Regex("""\bgsk_[A-Za-z0-9]{20,}"""),
        Regex("""\bcsk-[A-Za-z0-9]{20,}"""),
        Regex("""\bnvapi-[A-Za-z0-9_\-]{20,}"""),
        Regex("""\bAIza[0-9A-Za-z_\-]{30,}"""),
        Regex("""\b(?:ghp|gho|ghu|ghs|ghr|github_pat)_[A-Za-z0-9_]{20,}"""),
        Regex("""\bxox[abprs]-[A-Za-z0-9\-]{10,}"""),
        Regex("""\bAKIA[0-9A-Z]{16}\b"""),
        Regex("""\bsb_(?:secret|publishable)_[A-Za-z0-9_\-]{16,}"""),
        Regex("""\beyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}"""),
        Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----"""),
        Regex("""(?i)\bbearer\s+[A-Za-z0-9._\-]{16,}"""),
        // generic long random strings next to a key-ish word
        Regex("""(?i)(?<![\p{L}\p{N}])(?:api[_ -]?key|token|secret|access[_ -]?key|ключ|токен)(?![\p{L}\p{N}])\s*[:=]?\s*["']?[A-Za-z0-9_\-./+]{16,}["']?"""),
    )

    private val passwordPattern = Regex(
        """(?i)(?<![\p{L}\p{N}])(password|passwd|pwd|passcode|pin(?:\s*code)?|пароль|пин(?:-?код)?)(?![\p{L}\p{N}])(\s*(?:is|was|=|:|это|-)?\s*)(["']?)(\S{3,})\3"""
    )

    private val cardCandidate = Regex("""\b(?:\d[ \-]?){12,18}\d\b""")
    private val ibanPattern = Regex("""\b[A-Z]{2}\d{2}(?: ?[A-Z0-9]{4}){3,7}(?: ?[A-Z0-9]{1,3})?\b""")
    private val cvvPattern = Regex("""(?i)\b(cvv|cvc|cvv2|cvc2)\b\s*[:=]?\s*\d{3,4}\b""")

    fun scrub(text: String): String {
        var out = text
        for (p in patterns) out = p.replace(out, MASK)
        out = passwordPattern.replace(out) { m -> m.groupValues[1] + m.groupValues[2] + MASK }
        out = cvvPattern.replace(out) { m -> m.groupValues[1] + " " + MASK }
        out = cardCandidate.replace(out) { m ->
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 13..19 && luhn(digits)) MASK else m.value
        }
        out = ibanPattern.replace(out, MASK)
        return out
    }

    fun containsSecret(text: String): Boolean = scrub(text) != text

    fun luhn(digits: String): Boolean {
        var sum = 0
        var alt = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (n !in 0..9) return false
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }
}
