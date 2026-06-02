package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import jakarta.transaction.Transactional
import java.math.BigDecimal
import java.time.LocalDate
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

    fun findByActiveStatusOrderByTransactionDateDesc(activeStatus: Boolean = true): List<Transaction>

    fun findByCategoryAndActiveStatusOrderByTransactionDateDesc(
        category: String,
        activeStatus: Boolean = true
    ): List<Transaction>

    fun findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
        description: String,
        activeStatus: Boolean = true
    ): List<Transaction>

    fun findByTransactionDateBetweenAndActiveStatusOrderByTransactionDateDesc(
        startDate: LocalDate,
        endDate: LocalDate,
        activeStatus: Boolean = true
    ): List<Transaction>

    fun findByAccountNameOwnerAndActiveStatus(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        pageable: Pageable
    ): Page<Transaction>

    @Query(
        value = "SELECT COALESCE(SUM(t.amount), 0) FROM t_transaction t WHERE t.account_name_owner = :accountNameOwner AND t.transaction_state = :transactionState AND t.transaction_date BETWEEN :startDate AND :endDate AND t.active_status = true",
        nativeQuery = true
    )
    fun sumSpendingInWindow(
        accountNameOwner: String,
        transactionState: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): BigDecimal

    @Query(
        value = "SELECT COALESCE(SUM(t.amount), 0) FROM t_transaction t WHERE t.account_name_owner = :accountNameOwner AND t.transaction_state IN (:transactionStates) AND t.transaction_date BETWEEN :startDate AND :endDate AND t.active_status = true",
        nativeQuery = true
    )
    fun sumPendingSpendingInWindow(
        accountNameOwner: String,
        transactionStates: List<String>,
        startDate: LocalDate,
        endDate: LocalDate
    ): BigDecimal

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
