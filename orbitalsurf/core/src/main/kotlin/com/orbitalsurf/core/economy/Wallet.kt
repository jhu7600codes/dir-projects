package com.orbitalsurf.core.economy

import kotlinx.serialization.Serializable

/** The plates balance. Immutable -- every mutation returns a new `Wallet` rather than mutating in place. */
@Serializable
data class Wallet(val plates: Long = 0L) {
    fun credit(amount: Long): Wallet {
        require(amount >= 0) { "credit amount must be non-negative, got $amount" }
        return copy(plates = plates + amount)
    }

    /** Returns null (no partial debit, no side effect) if the balance can't cover [amount]. */
    fun debit(amount: Long): Wallet? {
        require(amount >= 0) { "debit amount must be non-negative, got $amount" }
        return if (plates >= amount) copy(plates = plates - amount) else null
    }
}
