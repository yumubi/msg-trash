package service

import io.vertx.core.json.JsonObject
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MetricsCollector {
    private val mutex = Mutex()
    private val messageCreated = AtomicLong(0)
    private val messageRead = AtomicLong(0)
    private val messageExpired = AtomicLong(0)
    private val messageDenied = AtomicLong(0)
    private val messageNotFound = AtomicLong(0)

    private val requestLatencies = ConcurrentHashMap<String, MutableList<Long>>()

    suspend fun recordMessageCreation() = mutex.withLock {
        messageCreated.incrementAndGet()
    }

    suspend fun recordMessageRead() = mutex.withLock {
        messageRead.incrementAndGet()
    }

    suspend fun recordMessageExpired() = mutex.withLock {
        messageExpired.incrementAndGet()
    }

    suspend fun recordMessageDenied() = mutex.withLock {
        messageDenied.incrementAndGet()
    }

    suspend fun recordMessageNotFound() = mutex.withLock {
        messageNotFound.incrementAndGet()
    }

    suspend fun recordLatency(endpoint: String, latencyMs: Long) = mutex.withLock {
        requestLatencies.computeIfAbsent(endpoint) { mutableListOf() }.add(latencyMs)
    }

    suspend fun getMetrics(): JsonObject = mutex.withLock {
        return JsonObject()
            .put("messages", JsonObject()
                .put("created", messageCreated.get())
                .put("read", messageRead.get())
                .put("expired", messageExpired.get())
                .put("denied", messageDenied.get())
                .put("not_found", messageNotFound.get())
            )
            .put("latencies", JsonObject().apply {
                requestLatencies.forEach { (endpoint, latencies) ->
                    put(endpoint, JsonObject()
                        .put("avg", latencies.average())
                        .put("min", latencies.minOrNull())
                        .put("max", latencies.maxOrNull())
                        .put("count", latencies.size)
                    )
                }
            })
    }
}
