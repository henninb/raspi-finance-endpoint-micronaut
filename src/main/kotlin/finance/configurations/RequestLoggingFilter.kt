package finance.configurations

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

@Filter("/**")
class RequestLoggingFilter : HttpServerFilter {

    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val start = System.currentTimeMillis()
        logger.info("Incoming request: {} {}", request.method, request.uri)
        return Flux.from(chain.proceed(request)).doOnNext { response ->
            val elapsed = System.currentTimeMillis() - start
            logger.info("Completed request: {} {} -> {} ({}ms)", request.method, request.uri, response.status.code, elapsed)
        }
    }
}
