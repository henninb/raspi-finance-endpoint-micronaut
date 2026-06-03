package finance.utils

import org.apache.logging.log4j.LogManager
import java.time.LocalDate
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class DateValidator : ConstraintValidator<ValidDate, LocalDate> {
    override fun initialize(constraintAnnotation: ValidDate) {
    }

    override fun isValid(value: LocalDate, context: ConstraintValidatorContext): Boolean {
        logger.debug("dateToBeEvaluated: $value")
        return value.isAfter(LocalDate.of(2000, 1, 1))
    }

    companion object {
        private val logger = LogManager.getLogger(DateValidator::class.java)
    }
}