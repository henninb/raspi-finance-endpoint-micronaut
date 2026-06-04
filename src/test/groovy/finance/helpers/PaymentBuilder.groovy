package finance.helpers

import finance.domain.Payment

import java.time.LocalDate

class PaymentBuilder {

    String sourceAccount = 'checking_brian'
    String destinationAccount = 'foo_brian'
    BigDecimal amount = 5.00G
    LocalDate transactionDate = LocalDate.of(2020, 12, 11)
    String guidSource = UUID.randomUUID()
    String guidDestination = UUID.randomUUID()
    Boolean activeStatus = true

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    Payment build() {
        Payment payment = new Payment().with {
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            return it
        }
        return payment
    }

    PaymentBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount
        return this
    }

    PaymentBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount
        return this
    }

    PaymentBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    PaymentBuilder withTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    PaymentBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource
        return this
    }

    PaymentBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination
        return this
    }

    PaymentBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
