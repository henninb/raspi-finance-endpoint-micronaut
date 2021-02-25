package finance.repositories

import finance.domain.ReceiptImage
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ReceiptImageRepository : JpaRepository<ReceiptImage, Long> {
    fun findByTransactionId(transactionId: Long): Optional<ReceiptImage>
}