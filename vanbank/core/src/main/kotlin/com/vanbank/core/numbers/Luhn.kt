package com.vanbank.core.numbers

/** Standard Luhn checksum, the same check digit scheme Visa/Mastercard/etc use. */
object Luhn {
    fun isValid(digits: String): Boolean {
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return false
        return checksum(digits) % 10 == 0
    }

    /** Given all digits except the final check digit, returns the check digit that makes it valid. */
    fun checkDigitFor(payload: String): Int {
        require(payload.all { it.isDigit() })
        val withPlaceholder = payload + "0"
        val sum = checksum(withPlaceholder)
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }

    private fun checksum(digits: String): Int {
        var sum = 0
        // Doubling starts from the rightmost digit, every second digit.
        for ((indexFromRight, char) in digits.reversed().withIndex()) {
            var d = char - '0'
            if (indexFromRight % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum
    }
}
