package finance.repositories

import finance.domain.Parameter
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

@Repository
interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByParameterName(parameterName: String): Optional<Parameter>

    @Transactional
    fun deleteByParameterName(parameterName: String)
}