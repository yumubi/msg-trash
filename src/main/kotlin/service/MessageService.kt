package service

import config.Config
import io.goji.service.MessageStorage
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.EncryptedData
import model.StoredMessage
import mu.KotlinLogging
import java.util.*

class MessageService(
    private val vertx: Vertx,
    private val storage: MessageStorage
) {
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(vertx.dispatcher())

    init {
        setupCleanupJob()
    }

    private fun setupCleanupJob() {
        val cleanupInterval = Config.messageConfig.getLong("cleanupInterval")
        vertx.setPeriodic(cleanupInterval) {
            scope.launch {
                try {
                    cleanup()
                } catch (e: Exception) {
                    logger.error(e) { "Error during cleanup" }
                }
            }
        }
    }

    suspend fun createMessage(content: EncryptedData, ttlMillis: Long?): String = mutex.withLock {
        val finalTtl = when {
            ttlMillis == null -> Config.messageConfig.getLong("defaultTtl")
            ttlMillis > Config.messageConfig.getLong("maxTtl") ->
                Config.messageConfig.getLong("maxTtl")
            else -> ttlMillis
        }

        val id = UUID.randomUUID().toString()
        val expirationTime = System.currentTimeMillis() + finalTtl
        val message = StoredMessage(content, expirationTime)

        storage.save(id, message)
        return id
    }

    suspend fun readAndDestroyMessage(id: String): StoredMessage? = mutex.withLock {
        val message = storage.get(id) ?: return null

        if (System.currentTimeMillis() >= message.expirationTime) {
            storage.delete(id)
            return null
        }

        storage.delete(id)
        return message
    }

    private suspend fun cleanup() = mutex.withLock {
        storage.cleanup(System.currentTimeMillis())
    }
}
