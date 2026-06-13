package finance.repositories

import finance.domain.Transfer
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface TransferRepository : JpaRepository<Transfer, Long> {
    fun findByTransferId(paymentId: Long): Optional<Transfer>

    fun findByActiveStatusOrderByTransactionDateDesc(activeStatus: Boolean = true, pageable: Pageable): Page<Transfer>
}