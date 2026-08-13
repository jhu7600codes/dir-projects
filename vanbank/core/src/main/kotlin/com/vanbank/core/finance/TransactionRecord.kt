package com.vanbank.core.finance

import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import java.time.Instant

/**
 * The minimal shape of a transaction that the pure statement/budget math
 * needs. The app module's Room entity carries a lot more (ids, card refs,
 * counterparty, status) and maps down to this for calculation.
 */
data class TransactionRecord(
    val timestamp: Instant,
    val amountMinor: Long,
    val direction: TransactionDirection,
    val category: TransactionCategory,
)
