package io.goji.io.goji.burnread.service

class MessageValidator {
    companion object {
        private val FORBIDDEN_PATTERNS = listOf(
            Regex("<script.*?>.*?</script>", RegexOption.IGNORE_CASE),
            Regex("javascript:", RegexOption.IGNORE_CASE),
            Regex("onload=", RegexOption.IGNORE_CASE)
        )

        private const val MAX_MESSAGE_LENGTH = 1024 * 1024 // 1MB
    }

    data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null
    )

    fun validate(message: String, ttl: Long, maxTtl: Long): ValidationResult {
        if (message.isEmpty()) {
            return ValidationResult(false, "Message cannot be empty")
        }

        if (message.length > MAX_MESSAGE_LENGTH) {
            return ValidationResult(false, "Message exceeds maximum length")
        }

        if (ttl <= 0) {
            return ValidationResult(false, "TTL must be positive")
        }

        if (ttl > maxTtl) {
            return ValidationResult(false, "TTL exceeds maximum allowed value")
        }

        FORBIDDEN_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(message)) {
                return ValidationResult(false, "Message contains forbidden content")
            }
        }

        return ValidationResult(true)
    }
}
