package finance.repositories

import finance.domain.Category
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategoryName(categoryName: String): Optional<Category>
    fun findByOwnerAndCategoryName(owner: String, categoryName: String): Optional<Category>

    fun findByActiveStatusOrderByCategoryName(activeStatus: Boolean): List<Category>
    fun findByOwnerAndActiveStatusOrderByCategoryName(owner: String, activeStatus: Boolean = true): List<Category>

    @Transactional
    fun deleteByCategoryName(categoryName: String)

    @Transactional
    fun deleteByOwnerAndCategoryName(owner: String, categoryName: String)
}
