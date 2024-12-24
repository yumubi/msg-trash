package service

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val maxRequests: Int = 100,
    private val perDuration: Duration = Duration.ofHours(1)
) {
    private val requestCounts = ConcurrentHashMap<String, RequestCount>()

    fun handler(): Handler<RoutingContext> = Handler { ctx ->
        val clientIp = ctx.request().remoteAddress().host()

        if (isAllowed(clientIp)) {
            ctx.next()
        } else {
            ctx.response()
                .setStatusCode(429)
                .putHeader("Content-Type", "application/json")
                .end("""{"error":"Too many requests","retry_after":${getRetryAfter(clientIp)}}""")
        }
    }


    fun tryAcquire(clientIp: String): Boolean {
        return isAllowed(clientIp)
    }

    private fun isAllowed(clientIp: String): Boolean {
        val now = System.currentTimeMillis()
        val count = requestCounts.compute(clientIp) { _, existing ->
            when {
                existing == null -> RequestCount(1, now)
                existing.windowStart + perDuration.toMillis() <= now -> RequestCount(1, now)
                existing.count >= maxRequests -> existing
                else -> existing.copy(count = existing.count + 1)
            }
        }

        return count!!.count <= maxRequests
    }

    private fun getRetryAfter(clientIp: String): Long {
        val count = requestCounts[clientIp] ?: return 0
        return (count.windowStart + perDuration.toMillis() - System.currentTimeMillis()) / 1000
    }

    private data class RequestCount(
        val count: Int,
        val windowStart: Long
    )
}
