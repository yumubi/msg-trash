package io.goji

import config.Config
import io.goji.service.InMemoryStorage
import io.goji.service.RedisStorage
import io.vertx.core.Vertx
import service.MessageService
import verticle.MainVerticle

fun main() {
    val vertx = Vertx.vertx()

    // 选择存储实现
    val storage = when (Config.storageConfig.getString("type")) {
        "redis" -> RedisStorage(vertx)
        else -> InMemoryStorage()
    }

    val messageService = MessageService(vertx, storage)
    vertx.deployVerticle(MainVerticle(vertx, messageService))
}
