package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Category
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
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
    @Inject val transactionRepository: TransactionRepository,
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
    open fun findByCategoryName(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategoryName(categoryName)
    }

    @Timed
    open fun deleteByCategoryName(categoryName: String): Boolean {
        categoryRepository.deleteByCategoryName(categoryName)
        return true
    }

    @Timed
    open fun fetchAllActiveCategories(): List<Category> {
        return categoryRepository.findByActiveStatusOrderByCategoryName(true)
    }

    @Timed
    open fun updateCategory(category: Category): Boolean {
        val existing = categoryRepository.findByCategoryName(category.categoryName)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.activeStatus = category.activeStatus
            toUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            categoryRepository.saveAndFlush(toUpdate)
            return true
        }
        return false
    }

    @Timed
    open fun mergeCategories(newCategory: String, oldCategory: String): Category {
        val newOpt = categoryRepository.findByCategoryName(newCategory)
        val oldOpt = categoryRepository.findByCategoryName(oldCategory)
        if (!newOpt.isPresent) throw RuntimeException("Category not found: $newCategory")
        if (!oldOpt.isPresent) throw RuntimeException("Category not found: $oldCategory")
        val updatedCount = transactionRepository.bulkUpdateCategory(oldCategory, newCategory)
        logger.info("Merged $updatedCount transactions from '$oldCategory' into '$newCategory'")
        val old = oldOpt.get()
        old.activeStatus = false
        old.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        categoryRepository.saveAndFlush(old)
        return newOpt.get()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
