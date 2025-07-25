package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

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

    //SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction
    //fun sumAmountBy
}
