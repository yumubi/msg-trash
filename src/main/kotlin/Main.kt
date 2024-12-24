import io.goji.io.goji.burnread.config.Config
import io.goji.io.goji.burnread.service.InMemoryStorage
import io.goji.io.goji.burnread.service.RedisStorage
import io.vertx.core.Vertx
import io.goji.io.goji.burnread.service.MessageService
import io.goji.io.goji.burnread.verticle.MainVerticle

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
