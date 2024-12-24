package model

data class StoredMessage(
    val content: EncryptedData,
    val expirationTime: Long
)


