package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val secureRandom = SecureRandom()

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val hash = pbkdf2(password.toCharArray(), salt)
        val encoder = Base64.getEncoder().withoutPadding()
        return "${encoder.encodeToString(salt)}:${encoder.encodeToString(hash)}"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        return try {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(parts[0])
            val expectedHash = decoder.decode(parts[1])
            val actualHash = pbkdf2(password.toCharArray(), salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
