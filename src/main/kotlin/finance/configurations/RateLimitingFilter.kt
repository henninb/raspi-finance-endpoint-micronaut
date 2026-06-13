package finance.configurations

import finance.utils.IpAddressValidator
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.scheduling.annotation.Scheduled
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Filter("/**")
class RateLimitingFilter : HttpServerFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(RateLimitingFilter::class.java)
        private const val MAX_REQUESTS_PER_WINDOW = 300
        private const val WINDOW_MS = 60_000L
        private val SKIP_PREFIXES = listOf("/health", "/actuator", "/metrics", "/info")
    }

    private data class Window(val count: AtomicInteger, val start: Instant)

    private val windows = ConcurrentHashMap<String, Window>()

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val path = request.uri.path
        if (SKIP_PREFIXES.any { path.startsWith(it) }) {
            return chain.proceed(request)
        }

        val ip = IpAddressValidator.getClientIpAddress(request)
        val now = Instant.now()

        val window = windows.compute(ip) { _, existing ->
            if (existing == null || now.toEpochMilli() - existing.start.toEpochMilli() > WINDOW_MS) {
                Window(AtomicInteger(1), now)
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!

        if (window.count.get() > MAX_REQUESTS_PER_WINDOW) {
            logger.warn("Rate limit exceeded: ip={} count={}", ip, window.count.get())
            @Suppress("UNCHECKED_CAST")
            return Flux.just(HttpResponse.status<Any>(HttpStatus.TOO_MANY_REQUESTS) as MutableHttpResponse<*>)
        }

        return chain.proceed(request)
    }

    @Scheduled(fixedDelay = "5m")
    fun evictExpiredWindows() {
        val cutoff = Instant.now().toEpochMilli() - WINDOW_MS
        windows.entries.removeIf { (_, w) -> w.start.toEpochMilli() < cutoff }
    }
}
