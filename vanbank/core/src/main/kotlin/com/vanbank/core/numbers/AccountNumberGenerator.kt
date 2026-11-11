package com.vanbank.core.numbers

import com.vanbank.core.model.AccountType
import kotlin.random.Random

/**
 * Generates 10-digit VANBank account numbers: a leading digit for the
 * account type (so a number is recognizable at a glance) followed by 9
 * random digits.
 */
object AccountNumberGenerator {
    private const val RANDOM_DIGITS = 9

    private fun typeDigit(type: AccountType): Char = when (type) {
        AccountType.CHECKING -> '1'
        AccountType.SAVINGS -> '2'
    }

    fun generate(type: AccountType, random: Random = Random.Default): String =
        buildString {
            append(typeDigit(type))
            repeat(RANDOM_DIGITS) { append(random.nextInt(0, 10)) }
        }

    fun accountTypeOf(accountNumber: String): AccountType? = when (accountNumber.firstOrNull()) {
        '1' -> AccountType.CHECKING
        '2' -> AccountType.SAVINGS
        else -> null
    }

    fun formatGrouped(accountNumber: String): String =
        accountNumber.chunked(4).joinToString(" ")
}
