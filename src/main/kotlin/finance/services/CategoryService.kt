package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Category
import finance.repositories.CategoryRepository
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
open class CategoryService(
    @Inject val categoryRepository: CategoryRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {

    @Timed
    open fun insertCategory(category: Category): Boolean {
        val constraintViolations: Set<ConstraintViolation<Category>> = validator.validate(category)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert category as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert category as there is a constraint violation on the data.")
        }
        category.dateAdded = Timestamp(Calendar.getInstance().time.time)
        category.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        categoryRepository.saveAndFlush(category)
        return true
    }

    @Timed
    open fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

    @Timed
    open fun deleteByCategoryName(categoryName: String): Boolean {
        categoryRepository.deleteByCategory(categoryName)
        return true
    }

    @Timed
    open fun fetchAllActiveCategories(): List<Category> {
        return categoryRepository.findByActiveStatusOrderByCategory(true)
    }

    @Timed
    open fun findByCategoryName(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategory(categoryName)
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }

}