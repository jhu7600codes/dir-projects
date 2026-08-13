package com.vanbank.app.data.repository

/** A user-facing failure reason (bad username, insufficient funds, frozen card, ...). */
class VanBankException(message: String) : Exception(message)

internal inline fun requireOrThrow(condition: Boolean, lazyMessage: () -> String) {
    if (!condition) throw VanBankException(lazyMessage())
}
