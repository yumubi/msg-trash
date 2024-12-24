package model

data class EncryptedData(
        val encryptedContent: String,
        val iv: String
    )
