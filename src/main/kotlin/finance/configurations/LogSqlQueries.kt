package finance.configurations

import io.micronaut.aop.Around
import jakarta.inject.Singleton

@Singleton
@Around
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class LogSqlQueries
