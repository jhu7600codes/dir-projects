package com.vanbank.core.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 password hashing for the fake login/signup flow. Nothing
 * here talks to a real identity provider -- it's just so "hashed in Room
 * db" is actually true rather than storing plaintext passwords.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    data class HashedPassword(val hashHex: String, val saltHex: String)

    fun hash(password: String, salt: ByteArray = randomSalt()): HashedPassword {
        val hash = pbkdf2(password, salt)
        return HashedPassword(hash.toHex(), salt.toHex())
    }

    fun verify(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val salt = saltHex.fromHex()
        val actual = pbkdf2(password, salt).toHex()
        return constantTimeEquals(actual, expectedHashHex)
    }

    private fun randomSalt(): ByteArray =
        ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
