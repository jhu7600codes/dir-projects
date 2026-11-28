package app.krala.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client side encryption for "sync keys": PBKDF2-HMAC-SHA256 (210k rounds) derives an AES-256
 * key from the passphrase, AES-GCM encrypts. Output: "k1:" + base64(salt | iv | ciphertext).
 * The server only ever sees this string.
 */
object PassphraseCrypto {
    private const val ITERATIONS = 210_000
    private const val PREFIX = "k1:"
    private val random = SecureRandom()

    fun encrypt(plain: String, passphrase: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(passphrase, salt), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(salt + iv + ct)
    }

    /** null when the passphrase is wrong or the data is damaged */
    fun decrypt(encoded: String, passphrase: String): String? = try {
        require(encoded.startsWith(PREFIX))
        val raw = Base64.getDecoder().decode(encoded.removePrefix(PREFIX))
        val salt = raw.copyOfRange(0, 16)
        val iv = raw.copyOfRange(16, 28)
        val ct = raw.copyOfRange(28, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(passphrase, salt), GCMParameterSpec(128, iv))
        String(cipher.doFinal(ct), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private fun derive(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }
}
