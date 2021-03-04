package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.*

import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Singleton
open class ParameterService(
    @Inject val parameterRepository: ParameterRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {
    @Timed
    open fun insertParameter(parameter: Parameter): Boolean {
        val constraintViolations: Set<ConstraintViolation<Parameter>> = validator.validate(parameter)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert parameter as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert parameter as there is a constraint violation on the data.")
        }

        parameter.dateAdded = Timestamp(Calendar.getInstance().time.time)
        parameter.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        parameterRepository.saveAndFlush(parameter)
        return true
    }

    @Timed
    open fun deleteByParameterName(parameterName: String) {
        parameterRepository.deleteByParameterName(parameterName)
    }

    @Timed
    open fun findByParameter(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}