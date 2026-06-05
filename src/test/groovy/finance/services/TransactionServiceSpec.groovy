package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import finance.utils.Constants
import org.hibernate.NonUniqueResultException

import jakarta.validation.ConstraintViolation
import java.time.LocalDate

@SuppressWarnings("GroovyAccessibility")
class TransactionServiceSpec extends BaseServiceSpec {

    protected Category category = CategoryBuilder.builder().build()

    void 'test transactionService - deleteByGuid'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted
        1 * transactionRepositoryMock.deleteByGuid(guid)
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - deleteByGuid - no record returned because of invalid guid'() {
        given:
        String guid = UUID.randomUUID()
        Optional<Transaction> transactionOptional = Optional.empty()

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        !isDeleted
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid - duplicates returned'() {
        given:
        String guid = UUID.randomUUID()

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        NonUniqueResultException ex = thrown()
        ex.message.toLowerCase().contains("query did not return a unique result")
        1 * transactionRepositoryMock.findByGuid(guid) >> { throw new NonUniqueResultException(2) }
        0 * _
    }

    void 'test transactionService - insert valid transaction'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        account.accountId = 1L
        account.accountType = AccountType.Credit
        Category cat = new Category()
        cat.categoryName = transaction.category
        Description desc = new Description()
        desc.descriptionName = transaction.description
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(_, transaction.category) >> Optional.of(cat)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(_, transaction.description) >> Optional.of(desc)
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test transactionService - attempt to insert duplicate transaction - update is called'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        Category cat = new Category()
        cat.categoryName = transaction.category
        Description desc = new Description()
        desc.descriptionName = transaction.description
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        constraintViolations.size() == 0
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(_, transaction.category) >> Optional.of(cat)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(_, transaction.description) >> Optional.of(desc)
        1 * transactionRepositoryMock.update({ Transaction entity ->
            assert entity.category == transaction.category
            assert entity.accountNameOwner == transaction.accountNameOwner
            assert entity.guid == transaction.guid
            assert entity.description == transaction.description
        })
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test transactionService - insert a valid transaction where category name does not exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        account.accountId = 1L
        account.accountType = AccountType.Credit
        Description desc = new Description()
        desc.descriptionName = transaction.description
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(_, transaction.category) >> Optional.empty()
        1 * validatorMock.validate(_ as Category) >> [].toSet()
        1 * categoryRepositoryMock.saveAndFlush(_)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(_, transaction.description) >> Optional.of(desc)
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- updateTransactionReoccurringState - not reoccurring'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Boolean isUpdated = transactionService.updateTransactionReoccurringFlag(transaction.guid, false)

        then:
        isUpdated
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring - monthly'() {
        given:
        Transaction transaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2020, 1, 15))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()
        transaction.transactionState = TransactionState.Cleared
        transaction.notes = 'my note will be removed'

        when:
        List<Transaction> transactions = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactions.size() == 2
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * transactionRepositoryMock.saveAndFlush({ Transaction futureTransaction ->
            assert futureTransaction.transactionState == TransactionState.Future
            assert futureTransaction.notes == ''
            assert futureTransaction.reoccurringType == ReoccurringType.Monthly
            futureTransaction
        }) >> transaction
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring - fortnightly'() {
        given:
        Transaction transaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2020, 1, 15))
                .withReoccurringType(ReoccurringType.FortNightly)
                .build()
        transaction.transactionState = TransactionState.Cleared
        transaction.notes = 'my note will be removed'

        when:
        List<Transaction> transactions = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactions.size() == 2
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * transactionRepositoryMock.saveAndFlush({ Transaction futureTransaction ->
            assert futureTransaction.transactionState == TransactionState.Future
            assert futureTransaction.notes == ''
            assert futureTransaction.reoccurringType == ReoccurringType.FortNightly
            futureTransaction
        }) >> transaction
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'create Future Transaction with jan 1 of leap year'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2020, 1, 1))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == LocalDate.of(2021, 1, 1)
        0 * _
    }

    void 'create Future Transaction with Feb 29'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2020, 2, 29))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == LocalDate.of(2021, 2, 28)
        0 * _
    }

    void 'create Future Transaction with leap year in play'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2019, 3, 1))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == LocalDate.of(2020, 3, 1)
        0 * _
    }

    void 'create Future Transaction with reoccurringType undefined'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2019, 11, 1))
                .withReoccurringType(ReoccurringType.Undefined)
                .build()

        when:
        transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        thrown(RuntimeException)
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }
}
