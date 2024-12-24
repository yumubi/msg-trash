package service

import model.EncryptedData
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService {
    private val algorithm = "AES/GCM/NoPadding"
    private val random = SecureRandom()
    private val key: SecretKey = generateKey()
    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 16

    fun encrypt(plaintext: String): EncryptedData {

        val iv = ByteArray(GCM_IV_LENGTH).apply {
            random.nextBytes(this)
        }

        val cipher = Cipher.getInstance(algorithm)

        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray())

        return EncryptedData(
            encryptedContent = Base64.getEncoder().encodeToString(encryptedBytes),
            iv = Base64.getEncoder().encodeToString(iv)

        )
    }

    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
        val iv = Base64.getDecoder().decode(encryptedData.iv)
        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, paramSpec)

        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData.encryptedContent))
        return String(decryptedBytes)
    }


    private fun generateKey(): SecretKey {
        // In production, this should be loaded from a secure key management system
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }
}
