package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.transaction.Transactional
import java.util.*

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByGuid(guid: String): Optional<Transaction>

    @Transactional
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true
    ): List<Transaction>

    fun findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        transactionStates: List<TransactionState>
    ): List<Transaction>

    @Transactional
    @Query(
        value = "UPDATE t_transaction SET category = :newCategory WHERE category = :oldCategory AND active_status = true",
        nativeQuery = true,
    )
    fun bulkUpdateCategory(oldCategory: String, newCategory: String): Int

    @Transactional
    @Query(
        value = "UPDATE t_transaction SET description = :newDescription WHERE description = :oldDescription AND active_status = true",
        nativeQuery = true,
    )
    fun bulkUpdateDescription(oldDescription: String, newDescription: String): Int
}
