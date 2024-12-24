package io.goji.io.goji.burnread.model
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
data class StoredMessage @JsonCreator constructor(
    @JsonProperty("content") val content: EncryptedData,
    @JsonProperty("expirationTime") val expirationTime: Long
)

