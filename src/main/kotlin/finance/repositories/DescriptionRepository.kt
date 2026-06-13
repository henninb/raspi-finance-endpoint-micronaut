package finance.repositories

import finance.domain.Description
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByActiveStatusOrderByDescriptionName(activeStatus: Boolean): List<Description>
    fun findByOwnerAndActiveStatusOrderByDescriptionName(owner: String, activeStatus: Boolean = true): List<Description>
    fun findByDescriptionName(descriptionName: String): Optional<Description>
    fun findByOwnerAndDescriptionName(owner: String, descriptionName: String): Optional<Description>

    @Transactional
    fun deleteByDescriptionName(descriptionName: String)

    @Transactional
    fun deleteByOwnerAndDescriptionName(owner: String, descriptionName: String)
}
