package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
open class BaseService {
    @Inject
    lateinit var meterService: MeterService

    // Simplified validation handling - would need proper validator in production
    fun handleConstraintViolations(constraintViolations: Set<Any>, meterService: MeterService) {
        if (constraintViolations.isNotEmpty()) {
            val details = "Validation constraint violation"
            logger.error("Cannot insert record because of constraint violation(s): $details")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw RuntimeException("Cannot insert record because of constraint violation(s): $details")
        }
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}