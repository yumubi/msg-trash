package io.goji.io.goji.burnread.verticle

import io.goji.io.goji.burnread.config.Config
import io.goji.io.goji.burnread.service.*
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class MainVerticle(
    private val vertx: Vertx,
    private val messageService: MessageService
) : CoroutineVerticle(), CoroutineRouterSupport {

    private val auditLogger = AuditLogger()
    private val encryptionService = EncryptionService()
    private val metricsCollector = MetricsCollector()
    private val rateLimiter = RateLimiter()
    private val healthCheck = HealthCheck()
    private val messageValidator = MessageValidator()

    override suspend fun start() {
        val router = Router.router(vertx)




//        val corsHandler = CorsHandler.create()
//            .addOrigin(Config.serverConfig.getString("corsAllowedOrigins"))
//            .allowedMethods(Config.serverConfig
//                .getJsonArray("corsAllowedMethods").map { HttpMethod.valueOf(it.toString()) }
//                .toSet())
//            .allowedHeaders(Config.serverConfig
//                .getJsonArray("corsAllowedHeaders").map { it.toString() }
//                .toSet())
//            .maxAgeSeconds(Config.serverConfig.getLong("corsMaxAge").toInt())
//
//        router.route().handler(corsHandler)

        // Configure CORS
        router.route().handler { ctx ->
            ctx.response()
                .putHeader("Access-Control-Allow-Origin", Config.serverConfig.getString("corsAllowedOrigins"))
                .putHeader("Access-Control-Allow-Methods", Config.serverConfig.getJsonArray("corsAllowedMethods").joinToString(","))
                .putHeader("Access-Control-Allow-Headers", Config.serverConfig.getJsonArray("corsAllowedHeaders").joinToString(","))
                .putHeader("Access-Control-Max-Age", Config.serverConfig.getLong("corsMaxAge").toString())
            ctx.next()
        }


        // Apply rate limiter to all routes
        router.route()
            .handler(BodyHandler.create())
            .handler(rateLimiter.handler())




        // Metrics endpoint
        router
            .get("/metrics").coHandler { ctx->
            val metrics = metricsCollector.getMetrics()
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(metrics.encode())
        }



        // Create message endpoint
        router.post("/api/messages").handler { ctx ->
            launch {
                try {
                    val body = ctx.body().asJsonObject()
                    val message = body.getString("message")

                    val ttl = body.getLong("ttl", Config.messageConfig.getLong("defaultTtl"))
                    // Validate message
                    val validateResult = messageValidator.validate(message, ttl, Config.messageConfig.getLong("maxTtl"))
                    if(!validateResult.isValid) {
                        ctx.response()
                            .setStatusCode(400)
                            .end("""{"error":"${validateResult.error}"}""")
                        return@launch
                    }

                    // Encrypt the message
                    val encryptedData = encryptionService.encrypt(message)
                    val id = messageService.createMessage(encryptedData, ttl)

                    // Record metrics and audit
                    metricsCollector.recordMessageCreation()
                    auditLogger.logMessageCreated(id, ttl, ctx.request().remoteAddress().host())


                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end("""{"id":"$id"}""")
                } catch (e: Exception) {
                    logger.error(e) { "Error creating message" }
                    ctx.response()
                        .setStatusCode(500)
                        .end("""{"error":"Internal Server Error"}""")
                }
            }
        }

        // Read and destroy message endpoint
        router.get("/api/messages/:id").handler { ctx ->
            launch {
                try {
                    val id = ctx.pathParam("id")

                    val clientIp = ctx.request().remoteAddress().host()

                    // Rate limiting check
                    if (!rateLimiter.tryAcquire(clientIp)) {
                        auditLogger.logAccessDenied(
                            clientIp,
                            "RATE_LIMIT_EXCEEDED",
                        )
                        ctx.response()
                            .setStatusCode(429)
                            .end("""{"error":"Too many requests"}""")
                        return@launch
                    }

                    val encryptedData = messageService.readAndDestroyMessage(id)


                    if (encryptedData != null) {
                        val message = encryptionService.decrypt(encryptedData.content)

                        // Record metrics and audit
                        metricsCollector.recordMessageRead()
                        auditLogger.logMessageRead(id, clientIp)

                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end("""{"message":"$message"}""")
                    } else {
                        // Record failed attempt
                        metricsCollector.recordMessageNotFound()
                        auditLogger.logMessageNotFound(id)
                        ctx.response()
                            .setStatusCode(404)
                            .end("""{"error":"Message not found or expired"}""")
                    }


                } catch (e: Exception) {
                    logger.error(e) { "Error reading message" }
                    metricsCollector.recordMessageNotFound()
                    auditLogger.logAccessDenied(
                        ctx.request().remoteAddress().host(),
                        "INTERNAL_ERROR"
                    )
                    ctx.response()
                        .setStatusCode(500)
                        .end("""{"error":"Internal Server Error"}""")
                }
            }
        }


        // Health check endpoint
        router.get("/health").handler { ctx ->
            launch {
                val health = healthCheck.checkHealth()
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(if (health.getString("status") == "OK") 200 else 503)
                    .end(health.encode())
            }
        }

        // Metrics endpoint
        router.get("/metrics").handler { ctx ->
            launch {
                val metrics = metricsCollector.getMetrics()
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(metrics.encode())
            }
        }

        // Start the server
        val port = System.getenv("PORT")?.toInt() ?: 5432
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port)
            .coAwait()

        logger.info { "Server started on port $port" }
    }
}
