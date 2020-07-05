package example.services

import example.domain.Category
import example.repositories.CategoryRepository
import example.repositories.TransactionRepository
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryService(@Inject val categoryRepository: CategoryRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertCategory(category: Category): Boolean {
        categoryRepository.save(category)
        return true
    }

    fun deleteByCategory(categoryName: String) {
        logger.info("deleteByCategory")
        categoryRepository.deleteByCategory(categoryName)
    }

    fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }
}