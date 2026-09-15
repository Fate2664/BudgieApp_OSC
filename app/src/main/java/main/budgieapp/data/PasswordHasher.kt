package main.budgieapp.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.exp

object PasswordHasher {
    const val ITERATIONS = 600_000

    data class Result(
        val hash: String,
        val salt: String,
        val iterations: Int
    )

    fun hash(password: CharArray): Result{
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val specification = PBEKeySpec(
            password,
            salt,
            ITERATIONS,
            256
        )

        return try {
            val hash = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded

            PasswordHasher.Result(
                hash = Base64.encodeToString(hash, Base64.NO_WRAP),
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                iterations = ITERATIONS
            )
        } finally {
            specification.clearPassword()
        }
    }

    fun verify(password: CharArray, user: UserEntity): Boolean{
        val salt = Base64.decode(user.passwordSalt, Base64.NO_WRAP)
        val expectedHash = Base64.decode(user.passwordHash, Base64.NO_WRAP)
        val specification = PBEKeySpec(
            password,
            salt,
            user.passwordIterations,
            256
        )

        return try {
            val actualHash = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded

            try {
                MessageDigest.isEqual(expectedHash, actualHash)
            } finally {
                actualHash.fill(0)
            }
        } finally {
            specification.clearPassword()
        }
    }
}