package finance.repositories

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
    fun findByTransactionStateAndAccountId(transactionState: TransactionState, accountId: Long): List<ValidationAmount>
    fun findByAccountId(accountId: Long): List<ValidationAmount>
}