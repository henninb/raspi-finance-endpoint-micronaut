package finance.repositories


import finance.domain.PendingTransaction
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface PendingTransactionRepository : JpaRepository<PendingTransaction, Long> {
    fun findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId: Long): Optional<PendingTransaction>
}