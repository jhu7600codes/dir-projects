package com.vanbank.core.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun `verify accepts the correct password`() {
        val hashed = PasswordHasher.hash("hunter2")
        assertTrue(PasswordHasher.verify("hunter2", hashed.saltHex, hashed.hashHex))
    }

    @Test
    fun `verify rejects the wrong password`() {
        val hashed = PasswordHasher.hash("hunter2")
        assertFalse(PasswordHasher.verify("wrong-password", hashed.saltHex, hashed.hashHex))
    }

    @Test
    fun `same password hashes differently each time due to random salt`() {
        val first = PasswordHasher.hash("hunter2")
        val second = PasswordHasher.hash("hunter2")
        assertNotEquals(first.hashHex, second.hashHex)
        assertNotEquals(first.saltHex, second.saltHex)
    }

    @Test
    fun `does not store the password in plaintext`() {
        val hashed = PasswordHasher.hash("hunter2")
        assertFalse(hashed.hashHex.contains("hunter2"))
    }
}
