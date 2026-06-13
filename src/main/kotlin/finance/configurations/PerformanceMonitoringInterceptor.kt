package finance.configurations

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@InterceptorBean(PerformanceMonitor::class)
class PerformanceMonitoringInterceptor : MethodInterceptor<Any, Any> {

    companion object {
        private val logger = LoggerFactory.getLogger(PerformanceMonitoringInterceptor::class.java)
    }

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val start = System.currentTimeMillis()
        return try {
            context.proceed()
        } finally {
            val elapsed = System.currentTimeMillis() - start
            val threshold = context.getAnnotationMetadata()
                .longValue(PerformanceMonitor::class.java, "thresholdMs")
                .orElse(500L)
            if (elapsed > threshold) {
                logger.warn(
                    "SLOW_METHOD class={} method={} elapsed={}ms threshold={}ms",
                    context.targetMethod.declaringClass.simpleName,
                    context.methodName,
                    elapsed,
                    threshold
                )
            } else {
                logger.debug(
                    "method={}.{}() elapsed={}ms",
                    context.targetMethod.declaringClass.simpleName,
                    context.methodName,
                    elapsed
                )
            }
        }
    }
}
