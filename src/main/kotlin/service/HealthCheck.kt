package service

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.management.ManagementFactory

class HealthCheck {
    private val mutex = Mutex()
    private var lastCheck = System.currentTimeMillis()
    private var status = "OK"


    suspend fun checkHealth(): JsonObject = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheck > 60000) { // Check every minute
            status = performHealthCheck()
            lastCheck = currentTime
        }

        val runtime = Runtime.getRuntime()
        val mb = 1024 * 1024

        return JsonObject()
            .put("status", status)
            .put("timestamp", currentTime)
            .put("memory", JsonObject()
                .put("total", runtime.totalMemory() / mb)
                .put("free", runtime.freeMemory() / mb)
                .put("max", runtime.maxMemory() / mb)
            )
            .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
    }

    private fun performHealthCheck(): String {
        return try {
            // Add specific health checks here
            // For example, check database connectivity, external services, etc.
            "OK"
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
