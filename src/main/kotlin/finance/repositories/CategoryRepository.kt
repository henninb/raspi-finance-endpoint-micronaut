package finance.repositories

import finance.domain.Category
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategoryName(categoryName: String): Optional<Category>

    fun findByActiveStatusOrderByCategoryName(activeStatus: Boolean): List<Category>

    @Transactional
    fun deleteByCategoryName(categoryName: String)
}
