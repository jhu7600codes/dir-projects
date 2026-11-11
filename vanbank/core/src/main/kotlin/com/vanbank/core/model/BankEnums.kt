package com.vanbank.core.model

/** Every account a VANBank user can hold. */
enum class AccountType {
    CHECKING,
    SAVINGS,
}

/** Debit draws straight from an account; credit draws against a limit. */
enum class CardType {
    DEBIT,
    CREDIT,
}

/**
 * Payment networks a card can be issued on. VANBank only ever issues DIR --
 * its custom network, identified the same way Visa (4) and Mastercard (5)
 * are: by the card number's first digit.
 */
enum class CardNetwork(val identifyingDigit: Char) {
    DIR('8'),
}

enum class CardStatus {
    ACTIVE,
    FROZEN,
}

/** Which way money moved on a transaction. */
enum class TransactionDirection {
    IN,
    OUT,
}

enum class TransactionStatus {
    COMPLETED,
    DECLINED,
    PENDING,
}

enum class TransactionCategory {
    GROCERIES,
    INCOME,
    SUBSCRIPTIONS,
    DINING,
    TRANSPORT,
    SHOPPING,
    UTILITIES,
    ENTERTAINMENT,
    RENT_MORTGAGE,
    TRANSFER,
    LOAN,
    SAVINGS,
    AI_SERVICES,
    FEES,
    OTHER,
}

enum class LoanStatus {
    PENDING,
    ACTIVE,
    PAID_OFF,
    REJECTED,
}

enum class BillFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class AiRequestStatus {
    PENDING,
    APPROVED,
    DECLINED,
}
