package finance.repositories

import finance.domain.Category
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategory(categoryName: String): Optional<Category>

    fun findByActiveStatusOrderByCategory(activeStatus: Boolean): List<Category>

    @Transactional
    fun deleteByCategory(categoryName: String)
}