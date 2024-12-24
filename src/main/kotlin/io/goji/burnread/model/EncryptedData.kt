package io.goji.io.goji.burnread.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class EncryptedData @JsonCreator constructor(
    @JsonProperty("encryptedContent") val encryptedContent: String,
    @JsonProperty("iv") val iv: String
)
