package finance.repositories

import finance.domain.Transfer
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface TransferRepository : JpaRepository<Transfer, Long> {
    fun findByTransferId(paymentId: Long): Optional<Transfer>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}