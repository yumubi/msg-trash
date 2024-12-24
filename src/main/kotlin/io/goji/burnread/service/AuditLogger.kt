package io.goji.io.goji.burnread.service

import mu.KotlinLogging
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AuditLogger {
    private val logger = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .withZone(ZoneOffset.UTC)

    fun logMessageCreated(messageId: String, ttl: Long, ip: String) {
        val event = createAuditEvent(
            AuditEvent.MESSAGE_CREATED,
            messageId,
            mapOf(
                "messageId" to messageId,
                "ttl" to ttl,
                "ip" to ip
            )
        )
        logger.info { event }
    }

    fun logMessageRead(messageId: String, ip: String) {
        val event = createAuditEvent(
            AuditEvent.MESSAGE_READ,
            messageId,
            mapOf(
                "messageId" to messageId,
                "ip" to ip
            )
        )
        logger.info { event }
    }

    fun logMessageExpired(messageId: String) {
        val event = createAuditEvent(
            AuditEvent.MESSAGE_EXPIRED,
            messageId,
            mapOf("messageId" to messageId)
        )
        logger.info { event }
    }


    fun logAccessDenied(userIp: String, reason: String) {
        val event = createAuditEvent(
            AuditEvent.ACCESS_DENIED,
            "N/A",
            mapOf(
                "ip" to userIp,
                "reason" to reason
            )
        )
        logger.info { event }
    }

    fun logMessageNotFound(messageId: String) {
        val event = createAuditEvent(
            AuditEvent.MESSAGE_NOT_FOUND,
            messageId,
            mapOf("messageId" to messageId)
        )
        logger.info { event }
    }


    private fun createAuditEvent(
        eventType: AuditEvent,
        messageId: String,
        details: Map<String, Any>
    ): String {
        return buildString {
            append("AUDIT:")
            append(" timestamp=\"").append(dateFormatter.format(Instant.now())).append("\"")
            append("messageId=$messageId, ")
            append(" event=\"").append(eventType).append("\"")
            details.forEach { (key, value) ->
                append(" ").append(key).append("=\"").append(value).append("\"")
            }
        }
    }




    enum class AuditEvent {
        MESSAGE_CREATED,
        MESSAGE_READ,
        MESSAGE_EXPIRED,
        ACCESS_DENIED,
        MESSAGE_NOT_FOUND,
        RATE_LIMIT_EXCEEDED,
    }
}
