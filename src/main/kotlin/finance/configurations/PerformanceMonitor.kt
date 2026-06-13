package finance.configurations

import io.micronaut.aop.Around

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Around
annotation class PerformanceMonitor(val thresholdMs: Long = 500L)
