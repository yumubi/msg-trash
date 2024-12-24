package io.goji.io.goji.burnread.service

import io.goji.io.goji.burnread.config.Config
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.goji.io.goji.burnread.model.StoredMessage
import java.time.Duration
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
    private val clientResources: ClientResources = DefaultClientResources.builder()
        .build()
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


        redisClient = RedisClient
            .create(clientResources, redisUri)
        redisClient.options = ClientOptions.builder()
            // 设置断开连接超时
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            // 设置超时选项
            .timeoutOptions(
                TimeoutOptions.builder()
                    .fixedTimeout(Duration.ofSeconds(10))
                    .build())
            // 设置是否启用自动重连
            .autoReconnect(true)
            .build()

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
    }

    fun close() {
        redisClient.shutdown()
    }
}
