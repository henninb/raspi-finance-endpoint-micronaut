package example.repositories

import example.domain.Transaction
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository

import java.util.*

import java.math.BigDecimal
import javax.transaction.Transactional

@Repository
interface TransactionRepository : CrudRepository<Transaction, Long> {
    //TODO: add LIMIT 1 result
    fun findByGuid(guid: String): Optional<Transaction>

    //fun find( transactionId: Long): Transaction

    //@Modifying
    @Query("UPDATE TransactionEntity set amount = ?1 WHERE guid = ?2")
    @Transactional
    fun setAmountByGuid(amount: BigDecimal, guild: String)

    //@Modifying
    @Query("UPDATE TransactionEntity set cleared = ?1 WHERE guid = ?2")
    @Transactional
    fun setClearedByGuid(cleared: Int, guild: String)
}
