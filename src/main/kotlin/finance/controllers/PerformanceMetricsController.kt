package finance.controllers

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit

@Controller("/performance")
class PerformanceMetricsController(@Inject private val meterRegistry: MeterRegistry) : BaseController() {

    @Get(value = "/summary", produces = [MediaType.APPLICATION_JSON])
    fun getPerformanceSummary(): HttpResponse<PerformanceSummary> {
        val methodMetrics = getMethodMetrics()
        val httpMetrics = getHttpMetrics()
        val dbMetrics = getDatabaseMetrics()
        val jvmMetrics = getJvmMetrics()
        return HttpResponse.ok(
            PerformanceSummary(
                methodExecutionMetrics = methodMetrics,
                httpRequestMetrics = httpMetrics,
                databaseMetrics = dbMetrics,
                jvmMetrics = jvmMetrics,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    @Get(value = "/methods", produces = [MediaType.APPLICATION_JSON])
    fun getMethodMetrics(): MethodExecutionMetrics {
        val timers = meterRegistry.meters
            .filter { it.id.name == "method.execution.time" }
            .mapNotNull { it as? Timer }

        val serviceMetrics = timers
            .filter { it.id.getTag("layer") == "service" }
            .map { buildMethodMetric(it) }
            .sortedByDescending { it.avgTimeMs }
            .take(20)

        val repositoryMetrics = timers
            .filter { it.id.getTag("layer") == "repository" }
            .map { buildMethodMetric(it) }
            .sortedByDescending { it.avgTimeMs }
            .take(20)

        val slowestMethods = timers
            .map { buildMethodMetric(it) }
            .sortedByDescending { it.maxTimeMs }
            .take(10)

        val mostCalledMethods = timers
            .map { buildMethodMetric(it) }
            .sortedByDescending { it.count }
            .take(10)

        return MethodExecutionMetrics(
            serviceLayerTop20 = serviceMetrics,
            repositoryLayerTop20 = repositoryMetrics,
            slowest10Overall = slowestMethods,
            mostCalled10 = mostCalledMethods,
        )
    }

    @Get(value = "/database", produces = [MediaType.APPLICATION_JSON])
    fun getDatabaseMetrics(): DatabaseMetrics {
        val active = meterRegistry.find("hikari.connections.active").gauge()?.value()?.toInt() ?: 0
        val idle = meterRegistry.find("hikari.connections.idle").gauge()?.value()?.toInt() ?: 0
        val pending = meterRegistry.find("hikari.connections.pending").gauge()?.value()?.toInt() ?: 0
        val total = meterRegistry.find("hikari.connections.total").gauge()?.value()?.toInt() ?: 0
        val circuitState = meterRegistry.meters
            .firstOrNull { it.id.name.contains("circuitbreaker.state") }
            ?.id?.getTag("state") ?: "unknown"

        return DatabaseMetrics(
            connectionPool = ConnectionPoolMetrics(
                active = active,
                idle = idle,
                pending = pending,
                total = total,
                utilizationPercent = if (total > 0) (active.toDouble() / total * 100) else 0.0,
            ),
            circuitBreakerState = circuitState,
        )
    }

    @Get(value = "/http", produces = [MediaType.APPLICATION_JSON])
    fun getHttpMetrics(): HttpRequestMetrics {
        val httpTimers = meterRegistry.meters
            .filter { it.id.name == "http.server.requests" }
            .mapNotNull { it as? Timer }

        val totalRequests = httpTimers.sumOf { it.count() }
        val statusCounts = httpTimers
            .groupBy { it.id.getTag("status") ?: "unknown" }
            .mapValues { (_, timers) -> timers.sumOf { it.count() } }

        val slowestEndpoints = httpTimers
            .sortedByDescending { it.max(TimeUnit.MILLISECONDS) }
            .take(10)
            .map { timer ->
                EndpointMetric(
                    method = timer.id.getTag("method") ?: "unknown",
                    uri = timer.id.getTag("uri") ?: "unknown",
                    status = timer.id.getTag("status") ?: "unknown",
                    count = timer.count(),
                    avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
                    maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
                )
            }

        val mostCalledEndpoints = httpTimers
            .sortedByDescending { it.count() }
            .take(10)
            .map { timer ->
                EndpointMetric(
                    method = timer.id.getTag("method") ?: "unknown",
                    uri = timer.id.getTag("uri") ?: "unknown",
                    status = timer.id.getTag("status") ?: "unknown",
                    count = timer.count(),
                    avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
                    maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
                )
            }

        return HttpRequestMetrics(
            totalRequests = totalRequests,
            statusCounts = statusCounts,
            slowestEndpoints = slowestEndpoints,
            mostCalledEndpoints = mostCalledEndpoints,
        )
    }

    @Get(value = "/jvm", produces = [MediaType.APPLICATION_JSON])
    fun getJvmMetrics(): JvmMetrics {
        val heapUsed = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge()?.value() ?: 0.0
        val heapMax = meterRegistry.find("jvm.memory.max").tag("area", "heap").gauge()?.value() ?: 0.0
        val nonHeapUsed = meterRegistry.find("jvm.memory.used").tag("area", "nonheap").gauge()?.value() ?: 0.0
        val nonHeapMax = meterRegistry.find("jvm.memory.max").tag("area", "nonheap").gauge()?.value() ?: 0.0

        val gcPauseTimer = meterRegistry.find("jvm.gc.pause").timer()
        val gcCount = gcPauseTimer?.count() ?: 0
        val gcTotalTimeMs = gcPauseTimer?.totalTime(TimeUnit.MILLISECONDS) ?: 0.0

        val threadCount = meterRegistry.find("jvm.threads.live").gauge()?.value()?.toInt() ?: 0
        val daemonThreadCount = meterRegistry.find("jvm.threads.daemon").gauge()?.value()?.toInt() ?: 0
        val cpuUsage = meterRegistry.find("system.cpu.usage").gauge()?.value() ?: 0.0
        val processCpuUsage = meterRegistry.find("process.cpu.usage").gauge()?.value() ?: 0.0

        return JvmMetrics(
            heapMemory = MemoryMetric(
                usedMB = heapUsed / 1024 / 1024,
                maxMB = heapMax / 1024 / 1024,
                usagePercent = if (heapMax > 0) (heapUsed / heapMax * 100) else 0.0,
            ),
            nonHeapMemory = MemoryMetric(
                usedMB = nonHeapUsed / 1024 / 1024,
                maxMB = nonHeapMax / 1024 / 1024,
                usagePercent = if (nonHeapMax > 0) (nonHeapUsed / nonHeapMax * 100) else 0.0,
            ),
            garbageCollection = GcMetrics(
                totalCollections = gcCount,
                totalPauseTimeMs = gcTotalTimeMs,
                avgPauseTimeMs = if (gcCount > 0) gcTotalTimeMs / gcCount else 0.0,
            ),
            threads = ThreadMetrics(live = threadCount, daemon = daemonThreadCount),
            cpu = CpuMetrics(systemUsagePercent = cpuUsage * 100, processUsagePercent = processCpuUsage * 100),
        )
    }

    private fun buildMethodMetric(timer: Timer): MethodMetric = MethodMetric(
        className = timer.id.getTag("class") ?: "Unknown",
        methodName = timer.id.getTag("method") ?: "Unknown",
        layer = timer.id.getTag("layer") ?: "unknown",
        status = timer.id.getTag("status") ?: "unknown",
        count = timer.count(),
        avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
        maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
        totalTimeMs = timer.totalTime(TimeUnit.MILLISECONDS),
    )

    data class PerformanceSummary(
        val methodExecutionMetrics: MethodExecutionMetrics,
        val httpRequestMetrics: HttpRequestMetrics,
        val databaseMetrics: DatabaseMetrics,
        val jvmMetrics: JvmMetrics,
        val timestamp: Long,
    )

    data class MethodExecutionMetrics(
        val serviceLayerTop20: List<MethodMetric>,
        val repositoryLayerTop20: List<MethodMetric>,
        val slowest10Overall: List<MethodMetric>,
        val mostCalled10: List<MethodMetric>,
    )

    data class MethodMetric(
        val className: String,
        val methodName: String,
        val layer: String,
        val status: String,
        val count: Long,
        val avgTimeMs: Double,
        val maxTimeMs: Double,
        val totalTimeMs: Double,
    )

    data class HttpRequestMetrics(
        val totalRequests: Long,
        val statusCounts: Map<String, Long>,
        val slowestEndpoints: List<EndpointMetric>,
        val mostCalledEndpoints: List<EndpointMetric>,
    )

    data class EndpointMetric(
        val method: String,
        val uri: String,
        val status: String,
        val count: Long,
        val avgTimeMs: Double,
        val maxTimeMs: Double,
    )

    data class DatabaseMetrics(
        val connectionPool: ConnectionPoolMetrics,
        val circuitBreakerState: String,
    )

    data class ConnectionPoolMetrics(
        val active: Int,
        val idle: Int,
        val pending: Int,
        val total: Int,
        val utilizationPercent: Double,
    )

    data class JvmMetrics(
        val heapMemory: MemoryMetric,
        val nonHeapMemory: MemoryMetric,
        val garbageCollection: GcMetrics,
        val threads: ThreadMetrics,
        val cpu: CpuMetrics,
    )

    data class MemoryMetric(val usedMB: Double, val maxMB: Double, val usagePercent: Double)
    data class GcMetrics(val totalCollections: Long, val totalPauseTimeMs: Double, val avgPauseTimeMs: Double)
    data class ThreadMetrics(val live: Int, val daemon: Int)
    data class CpuMetrics(val systemUsagePercent: Double, val processUsagePercent: Double)
}
