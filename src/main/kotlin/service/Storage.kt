package io.goji.service

import config.Config
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import model.StoredMessage
import java.util.concurrent.ConcurrentHashMap

interface MessageStorage {
    suspend fun save(id: String, message: StoredMessage)
    suspend fun get(id: String): StoredMessage?
    suspend fun delete(id: String)
    suspend fun cleanup(currentTime: Long)
}

class InMemoryStorage : MessageStorage {
    private val messages = ConcurrentHashMap<String, StoredMessage>()

    override suspend fun save(id: String, message: StoredMessage) {
        messages[id] = message
    }

    override suspend fun get(id: String): StoredMessage? = messages[id]

    override suspend fun delete(id: String) {
        messages.remove(id)
    }

    override suspend fun cleanup(currentTime: Long) {
        messages.entries.removeIf { (_, message) ->
            currentTime >= message.expirationTime
        }
    }
}

class RedisStorage(private val vertx: Vertx) : MessageStorage {
    private val redisClient: RedisClient
    private val redisCommands: RedisCoroutinesCommands<String, String>

    init {
        val redisConfig = Config.storageConfig.getJsonObject("redis")
        val redisUri = RedisURI.builder()
            .withHost(redisConfig.getString("host"))
            .withPort(redisConfig.getInteger("port"))
            .withPassword(redisConfig.getString("password").toCharArray())
            .withDatabase(redisConfig.getInteger("database"))
            .build()

        redisClient = RedisClient.create(redisUri)
        val connection = redisClient.connect()
        redisCommands = connection.coroutines()
    }

    override suspend fun save(id: String, message: StoredMessage) {
        val ttlMillis = message.expirationTime - System.currentTimeMillis()
        if (ttlMillis > 0) {
            redisCommands.psetex(
                id,
                ttlMillis,
                Json.encode(message)
            )
        }
    }

    override suspend fun get(id: String): StoredMessage? {
        return redisCommands.get(id)?.let { json ->
            Json.decodeValue(json, StoredMessage::class.java)
        }
    }

    override suspend fun delete(id: String) {
        redisCommands.del(id)
    }

    override suspend fun cleanup(currentTime: Long) {
        // Redis会自动通过TTL清理过期消息，无需实现
    }

    fun close() {
        redisClient.shutdown()
    }
}
