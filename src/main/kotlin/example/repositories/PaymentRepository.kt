package example.repositories


import example.domain.Payment
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Repository
interface PaymentRepository : CrudRepository<Payment, Long> {
    fun deleteByPaymentId(paymentId: Long)
    fun findByPaymentId(paymentId: Long): Optional<Payment>
}