package com.vanbank.core.numbers

import com.vanbank.core.model.CardNetwork
import kotlin.random.Random

/**
 * Generates 16-digit DIR card numbers: prefix '8' (DIR's identifying digit,
 * the way Visa uses 4 and Mastercard uses 5), 14 random digits, and a real
 * Luhn check digit -- so anything downstream that validates card numbers the
 * standard way accepts them.
 */
object CardNumberGenerator {
    private const val LENGTH = 16

    fun generate(network: CardNetwork = CardNetwork.DIR, random: Random = Random.Default): String {
        val payloadLength = LENGTH - 1 // last digit is the Luhn check digit
        val body = buildString {
            append(network.identifyingDigit)
            repeat(payloadLength - 1) { append(random.nextInt(0, 10)) }
        }
        val checkDigit = Luhn.checkDigitFor(body)
        return body + checkDigit
    }

    fun generateCvv(random: Random = Random.Default): String =
        random.nextInt(100, 1000).toString()

    fun isDirCard(cardNumber: String): Boolean =
        cardNumber.isNotEmpty() && cardNumber[0] == CardNetwork.DIR.identifyingDigit && Luhn.isValid(cardNumber)

    /** Formats as "8123 4567 8901 2345" for display. */
    fun formatGrouped(cardNumber: String): String =
        cardNumber.chunked(4).joinToString(" ")

    /** Formats as "•••• •••• •••• 2345" for anywhere the full PAN shouldn't show. */
    fun formatMasked(cardNumber: String): String {
        val last4 = cardNumber.takeLast(4)
        return "•••• •••• •••• $last4"
    }
}
