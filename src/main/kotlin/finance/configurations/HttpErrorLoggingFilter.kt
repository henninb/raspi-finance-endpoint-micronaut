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
class HttpErrorLoggingFilter : HttpServerFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(HttpErrorLoggingFilter::class.java)
    }

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val ip = IpAddressValidator.getClientIpAddress(request)
        val method = request.method
        val path = request.uri.path

        return Flux.from(chain.proceed(request)).doOnNext { response ->
            val status = response.status.code
            when {
                status >= 500 -> logger.error("SERVER_ERROR status={} method={} path={} ip={}", status, method, path, ip)
                status >= 400 -> logger.warn("CLIENT_ERROR status={} method={} path={} ip={}", status, method, path, ip)
            }
        }
    }
}
