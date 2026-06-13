package finance.repositories

import finance.domain.Parameter
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByParameterName(parameterName: String): Optional<Parameter>
    fun findByActiveStatusOrderByParameterName(activeStatus: Boolean = true): List<Parameter>

    fun findByOwnerAndParameterName(owner: String, parameterName: String): Optional<Parameter>
    fun findByOwnerAndActiveStatusOrderByParameterName(owner: String, activeStatus: Boolean = true): List<Parameter>

    @Transactional
    fun deleteByParameterName(parameterName: String)

    @Transactional
    fun deleteByOwnerAndParameterName(owner: String, parameterName: String)
}