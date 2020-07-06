package example.services

import example.domain.Payment
import example.repositories.PaymentRepository
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentService(@Inject val paymentRepository: PaymentRepository) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun findAllPayments(): MutableIterable<Payment> {
        return paymentRepository.findAll()
    }

    fun insertPayment(payment: Payment): Boolean {
        paymentRepository.save(payment)
        return true
    }

    fun deleteByPaymentId(paymentId: Long) {
        logger.info("service - deleteByPaymentId = $paymentId")
        paymentRepository.deleteByPaymentId(paymentId)
    }

    fun findByPaymentId(paymentId: Long): Optional<Payment> {
        logger.info("service - findByPaymentId = $paymentId")
        val paymentOptional: Optional<Payment> = paymentRepository.findByPaymentId(paymentId)
        if (paymentOptional.isPresent) {
            return paymentOptional
        }
        return Optional.empty()
    }
}