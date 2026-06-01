package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.PaymentRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator

@Singleton
open class PaymentService(
    @Inject val paymentRepository: PaymentRepository,
    @Inject val transactionService: TransactionService,
    @Inject val accountService: AccountService,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {

    @Timed
    open fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
    }

    @Timed
    open fun insertPayment(payment: Payment): Boolean {
        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert payment as there is a constraint violation on the data")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert payment as there is a constraint violation on the data")
        }

        val optionalDestinationAccount = accountService.findByAccountNameOwner(payment.destinationAccount)
        if (!optionalDestinationAccount.isPresent) {
            logger.error("Destination account not found ${payment.destinationAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Destination account not found ${payment.destinationAccount}")
        }

        val optionalSourceAccount = accountService.findByAccountNameOwner(payment.sourceAccount)
        if (!optionalSourceAccount.isPresent) {
            logger.error("Source account not found ${payment.sourceAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Source account not found ${payment.sourceAccount}")
        }

        val transactionDebit = Transaction()
        val transactionCredit = Transaction()

        populateDebitTransaction(transactionDebit, payment)
        populateCreditTransaction(transactionCredit, payment)

        transactionService.insertTransaction(transactionDebit)
        transactionService.insertTransaction(transactionCredit)

        payment.guidDestination = transactionCredit.guid
        payment.guidSource = transactionDebit.guid
        payment.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        payment.dateAdded = Timestamp(Calendar.getInstance().time.time)
        paymentRepository.saveAndFlush(payment)
        return true
    }

    @Timed
    open fun populateDebitTransaction(transactionDebit: Transaction, payment: Payment) {
        transactionDebit.guid = UUID.randomUUID().toString()
        transactionDebit.transactionDate = Date.valueOf(payment.transactionDate)
        transactionDebit.description = "payment"
        transactionDebit.category = "bill_pay"
        transactionDebit.notes = "to ${payment.destinationAccount}"
        transactionDebit.amount = if (payment.amount > BigDecimal(0.0)) payment.amount * BigDecimal(-1.0) else payment.amount
        transactionDebit.transactionState = TransactionState.Outstanding
        transactionDebit.accountType = AccountType.Debit
        transactionDebit.reoccurringType = ReoccurringType.Onetime
        transactionDebit.accountNameOwner = payment.sourceAccount
        transactionDebit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionDebit.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    open fun populateCreditTransaction(transactionCredit: Transaction, payment: Payment) {
        transactionCredit.guid = UUID.randomUUID().toString()
        transactionCredit.transactionDate = Date.valueOf(payment.transactionDate)
        transactionCredit.description = "payment"
        transactionCredit.category = "bill_pay"
        transactionCredit.notes = "from ${payment.sourceAccount}"
        transactionCredit.amount = if (payment.amount > BigDecimal(0.0)) payment.amount * BigDecimal(-1.0) else payment.amount
        transactionCredit.transactionState = TransactionState.Outstanding
        transactionCredit.accountType = AccountType.Credit
        transactionCredit.reoccurringType = ReoccurringType.Onetime
        transactionCredit.accountNameOwner = payment.destinationAccount
        transactionCredit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionCredit.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    open fun deleteByPaymentId(paymentId: Long) {
        logger.info("service - deleteByPaymentId = $paymentId")
        paymentRepository.deleteByPaymentId(paymentId)
    }

    @Timed
    open fun findByPaymentId(paymentId: Long): Optional<Payment> {
        logger.info("service - findByPaymentId = $paymentId")
        val paymentOptional: Optional<Payment> = paymentRepository.findByPaymentId(paymentId)
        if (paymentOptional.isPresent) {
            return paymentOptional
        }
        return Optional.empty()
    }

    @Timed
    open fun updatePayment(payment: Payment): Boolean {
        val existing = paymentRepository.findByPaymentId(payment.paymentId)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.sourceAccount = payment.sourceAccount
            toUpdate.destinationAccount = payment.destinationAccount
            toUpdate.amount = payment.amount
            toUpdate.transactionDate = payment.transactionDate
            toUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            paymentRepository.saveAndFlush(toUpdate)
            return true
        }
        return false
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
