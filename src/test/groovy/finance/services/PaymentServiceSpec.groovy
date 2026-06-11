package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.helpers.TransactionBuilder
import finance.utils.Constants

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class PaymentServiceSpec extends BaseServiceSpec {

    void 'test findAll payments - returns list'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        List<Payment> payments = [payment]

        when:
        List<Payment> results = paymentService.findAllPayments()

        then:
        results.size() == 1
        1 * paymentRepositoryMock.findAll() >> payments
        0 * _
    }

    void 'test insertPayment - both accounts found, transaction already exists'() {
        given:
        Payment payment = PaymentBuilder.builder().withAmount(5.0).build()
        Transaction transaction = TransactionBuilder.builder().build()
        Account sourceAccount = AccountBuilder.builder().withAccountNameOwner(payment.sourceAccount).build()
        Account destinationAccount = AccountBuilder.builder().withAccountNameOwner(payment.destinationAccount).build()
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        Boolean isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.of(destinationAccount)
        1 * accountRepositoryMock.findByAccountNameOwner(payment.sourceAccount) >> Optional.of(sourceAccount)
        2 * validatorMock.validate(_ as Transaction) >> [].toSet()
        2 * transactionRepositoryMock.findByGuid(_ as String) >> Optional.of(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * paymentRepositoryMock.saveAndFlush(payment)
        0 * _
    }

    void 'test insertPayment - destination account not found throws RuntimeException'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        paymentService.insertPayment(payment)

        then:
        thrown(RuntimeException)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.empty()
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertPayment - source account not found throws RuntimeException'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Account destinationAccount = AccountBuilder.builder().withAccountNameOwner(payment.destinationAccount).build()
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        paymentService.insertPayment(payment)

        then:
        thrown(RuntimeException)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.of(destinationAccount)
        1 * accountRepositoryMock.findByAccountNameOwner(payment.sourceAccount) >> Optional.empty()
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertPayment - validation failure throws ValidationException'() {
        given:
        Payment payment = PaymentBuilder.builder().withSourceAccount('x').build()
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        paymentService.insertPayment(payment)

        then:
        thrown(ValidationException)
        constraintViolations.size() > 0
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test deleteByPaymentId'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        payment.paymentId = 1L
        payment.guidSource = null
        payment.guidDestination = null
        Long paymentId = 1L

        when:
        paymentService.deleteByPaymentId(paymentId)

        then:
        1 * paymentRepositoryMock.findByPaymentId(paymentId) >> Optional.of(payment)
        1 * paymentRepositoryMock.deleteByPaymentId(paymentId)
        0 * _
    }

    void 'test findByPaymentId - found'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        payment.paymentId = 1L

        when:
        Optional<Payment> result = paymentService.findByPaymentId(1L)

        then:
        result.isPresent()
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(payment)
        0 * _
    }

    void 'test findByPaymentId - not found'() {
        when:
        Optional<Payment> result = paymentService.findByPaymentId(999L)

        then:
        !result.isPresent()
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        0 * _
    }

    void 'test updatePayment - found and updated'() {
        given:
        Payment existing = PaymentBuilder.builder().build()
        existing.paymentId = 1L
        Payment updated = PaymentBuilder.builder().withAmount(10.0).build()
        updated.paymentId = 1L

        when:
        Boolean result = paymentService.updatePayment(updated)

        then:
        result
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(existing)
        1 * paymentRepositoryMock.saveAndFlush(existing)
        0 * _
    }

    void 'test updatePayment - not found returns false'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        payment.paymentId = 999L

        when:
        Boolean result = paymentService.updatePayment(payment)

        then:
        !result
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        0 * _
    }

    void 'test populateDebitTransaction sets correct fields'() {
        given:
        Payment payment = PaymentBuilder.builder().withAmount(5.0).build()
        Transaction transaction = new Transaction()

        when:
        paymentService.populateDebitTransaction(transaction, payment)

        then:
        transaction.category == 'bill_pay'
        transaction.description == 'payment'
        transaction.notes == 'to ' + payment.destinationAccount
        transaction.amount == (payment.amount * -1.0)
        transaction.accountType == AccountType.Debit
        transaction.accountNameOwner == payment.sourceAccount
        0 * _
    }

    void 'test populateCreditTransaction sets correct fields'() {
        given:
        Payment payment = PaymentBuilder.builder().withAmount(5.0).build()
        Transaction transaction = new Transaction()

        when:
        paymentService.populateCreditTransaction(transaction, payment)

        then:
        transaction.category == 'bill_pay'
        transaction.description == 'payment'
        transaction.notes == 'from ' + payment.sourceAccount
        transaction.amount == (payment.amount * -1.0)
        transaction.accountType == AccountType.Credit
        transaction.accountNameOwner == payment.destinationAccount
        0 * _
    }
}
