package com.vaultgame.core.economy

import kotlinx.serialization.Serializable

/** The soft currency balance. "Plates" are earned per run from coins collected -- see
 * RunResultApplier -- and never purchasable with real money (cosmetics/convenience only). */
@Serializable
data class Wallet(val plates: Long = 0) {
    fun credit(amount: Long): Wallet {
        require(amount >= 0) { "credit amount must be non-negative, got $amount" }
        return copy(plates = plates + amount)
    }

    /** Returns null if [amount] exceeds the balance -- callers should check [canAfford] first
     * or handle the null themselves; this never goes negative. */
    fun debit(amount: Long): Wallet? {
        require(amount >= 0) { "debit amount must be non-negative, got $amount" }
        return if (amount > plates) null else copy(plates = plates - amount)
    }

    fun canAfford(amount: Long): Boolean = amount <= plates
}
