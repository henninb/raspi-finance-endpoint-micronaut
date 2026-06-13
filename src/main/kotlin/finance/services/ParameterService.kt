package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator

@Singleton
open class ParameterService(
    @Inject val parameterRepository: ParameterRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {
    @Timed
    open fun insertParameter(parameter: Parameter): Boolean {
        val constraintViolations: Set<ConstraintViolation<Parameter>> = validator.validate(parameter)
        if (!constraintViolations.isEmpty()) {
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
    open fun findAllActive(): List<Parameter> =
        parameterRepository.findByActiveStatusOrderByParameterName()

    @Timed
    open fun findAllActive(owner: String): List<Parameter> =
        parameterRepository.findByOwnerAndActiveStatusOrderByParameterName(owner)

    @Timed
    open fun findByParameter(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }

    @Timed
    open fun findByParameter(owner: String, parameterName: String): Optional<Parameter> =
        parameterRepository.findByOwnerAndParameterName(owner, parameterName)

    @Timed
    open fun deleteByParameterName(owner: String, parameterName: String) {
        parameterRepository.deleteByOwnerAndParameterName(owner, parameterName)
    }

    @Timed
    open fun updateParameter(parameterName: String, parameter: Parameter): Boolean {
        val optional = parameterRepository.findByParameterName(parameterName)
        if (!optional.isPresent) {
            logger.warn("Parameter not found: $parameterName")
            return false
        }
        val existing = optional.get()
        existing.parameterValue = parameter.parameterValue
        existing.activeStatus = parameter.activeStatus
        existing.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        parameterRepository.saveAndFlush(existing)
        return true
    }

    @Timed
    open fun updateParameter(owner: String, parameterName: String, parameter: Parameter): Boolean {
        val optional = parameterRepository.findByOwnerAndParameterName(owner, parameterName)
        if (!optional.isPresent) {
            logger.warn("Parameter not found for owner=$owner name=$parameterName")
            return false
        }
        val existing = optional.get()
        existing.parameterValue = parameter.parameterValue
        existing.activeStatus = parameter.activeStatus
        existing.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        parameterRepository.saveAndFlush(existing)
        return true
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger(ParameterService::class.java)
    }
}