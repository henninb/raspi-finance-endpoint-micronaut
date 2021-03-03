package finance.services

import finance.domain.Payment
import finance.repositories.PaymentRepository
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

//(paymentRepositoryMock, transactionService, accountService, parameterService, validatorMock, meterService)

@Singleton
class PaymentService(@Inject val paymentRepository: PaymentRepository,
                     @Inject val transactionService: TransactionService,
                     @Inject val accountService: AccountService,
                     @Inject val parameterService: ParameterService,
                     @Inject val validator: Validator,
                     @Inject val meterService: MeterService) {

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