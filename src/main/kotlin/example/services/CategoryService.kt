package example.services

import example.domain.Category
import example.repositories.CategoryRepository
import example.repositories.TransactionRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryService(@Inject val categoryRepository: CategoryRepository) {

    fun insertCategory(category: Category): Boolean {
        val categoryOptional = findByCategory(category.category)
        if( !categoryOptional.isPresent ) {
            categoryRepository.save(category)
        }
        return true
    }

    fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

}