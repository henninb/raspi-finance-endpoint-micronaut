package finance.repositories

import finance.domain.Payment
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentId(paymentId: Long): Optional<Payment>

    fun findByActiveStatusOrderByTransactionDateDesc(activeStatus: Boolean = true, pageable: Pageable): Page<Payment>

    fun findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner: String, activeStatus: Boolean = true): List<Payment>
    fun findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner: String, activeStatus: Boolean = true, pageable: Pageable): Page<Payment>
    fun findByOwnerAndPaymentId(owner: String, paymentId: Long): Optional<Payment>

    @Transactional
    fun deleteByPaymentId(paymentId: Long)
}