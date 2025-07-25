package finance.repositories

import finance.domain.Payment
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentId(paymentId: Long): Optional<Payment>

    @Transactional
    fun deleteByPaymentId(paymentId: Long)
}