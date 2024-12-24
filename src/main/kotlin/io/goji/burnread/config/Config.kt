package io.goji.io.goji.burnread.config

import com.typesafe.config.ConfigFactory
import io.vertx.core.json.JsonObject

object Config {
    private val config = ConfigFactory.load()

    val serverConfig = JsonObject().apply {
        put("port", config.getInt("server.port"))
        put("host", config.getString("server.host"))
        put("corsAllowedOrigins", config.getStringList("server.corsAllowedOrigins"))
        put("corsAllowedMethods", config.getStringList("server.corsAllowedMethods"))
        put("corsAllowedHeaders", config.getStringList("server.corsAllowedHeaders"))
        put("corsMaxAge", config.getLong("server.corsMaxAge"))
    }

    val messageConfig = JsonObject().apply {
        put("defaultTtl", config.getLong("message.defaultTtl"))
        put("maxTtl", config.getLong("message.maxTtl"))
        put("cleanupInterval", config.getLong("message.cleanupInterval"))
    }

    val storageConfig = JsonObject().apply {
        put("type", config.getString("storage.type"))
        put("redis", JsonObject().apply {
            put("host", config.getString("storage.redis.host"))
            put("port", config.getInt("storage.redis.port"))
            put("password", config.getString("storage.redis.password"))
            put("database", config.getInt("storage.redis.database"))
        })
    }
}
