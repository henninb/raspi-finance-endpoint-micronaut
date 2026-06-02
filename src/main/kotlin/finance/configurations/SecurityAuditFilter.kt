package finance.configurations

import finance.utils.IpAddressValidator
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

@Filter("/**")
class SecurityAuditFilter : HttpServerFilter {

    private val logger = LoggerFactory.getLogger(SecurityAuditFilter::class.java)

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val clientIp = IpAddressValidator.getClientIpAddress(request)
        return Flux.from(chain.proceed(request)).doOnNext { response ->
            val status = response.status.code
            if (status == 401 || status == 403) {
                logger.warn("Security event: {} {} from ip={} status={}", request.method, request.uri, clientIp, status)
            }
        }
    }
}
