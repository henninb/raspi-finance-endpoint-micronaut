package finance.configurations

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@InterceptorBean(LogSqlQueries::class)
class SqlQueryLoggingInterceptor : MethodInterceptor<Any, Any> {

    private val logger = LoggerFactory.getLogger(SqlQueryLoggingInterceptor::class.java)

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val start = System.currentTimeMillis()
        return try {
            val result = context.proceed()
            val elapsed = System.currentTimeMillis() - start
            logger.debug("SQL query via {}.{}() completed in {}ms", context.targetMethod.declaringClass.simpleName, context.methodName, elapsed)
            result
        } catch (e: Exception) {
            logger.error("SQL query via {}.{}() failed: {}", context.targetMethod.declaringClass.simpleName, context.methodName, e.message)
            throw e
        }
    }
}
