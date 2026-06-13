package finance.repositories

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
    fun findByTransactionStateAndAccountId(transactionState: TransactionState, accountId: Long): List<ValidationAmount>
    fun findByAccountId(accountId: Long): List<ValidationAmount>
    fun findByActiveStatusTrue(): List<ValidationAmount>

    fun findByOwnerAndActiveStatusTrue(owner: String): List<ValidationAmount>
    fun findByOwnerAndValidationId(owner: String, validationId: Long): Optional<ValidationAmount>
    fun findByOwnerAndTransactionStateAndAccountId(owner: String, transactionState: TransactionState, accountId: Long): List<ValidationAmount>
}