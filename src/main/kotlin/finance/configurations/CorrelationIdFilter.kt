package finance.configurations

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.util.UUID

@Filter("/**")
class CorrelationIdFilter : HttpServerFilter {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    }

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val correlationId = request.headers.get(CORRELATION_ID_HEADER) ?: UUID.randomUUID().toString()
        return Flux.from(chain.proceed(request)).doOnNext { response ->
            response.header(CORRELATION_ID_HEADER, correlationId)
        }
    }
}
