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
import jakarta.validation.ValidationException
import java.math.BigDecimal
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

    void 'test updateTransaction - valid update succeeds'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Category cat = new Category()
        cat.categoryName = transaction.category
        Description desc = new Description()
        desc.descriptionName = transaction.description
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean result = transactionService.updateTransaction(transaction)

        then:
        result
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(_, transaction.category) >> Optional.of(cat)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(_, transaction.description) >> Optional.of(desc)
        1 * transactionRepositoryMock.update(_ as Transaction)
        0 * _
    }

    void 'test updateTransaction - guid not found returns false'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)
        Category cat = new Category()
        cat.categoryName = transaction.category
        Description desc = new Description()
        desc.descriptionName = transaction.description

        when:
        Boolean result = transactionService.updateTransaction(transaction)

        then:
        !result
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        0 * _
    }

    void 'test updateTransaction - validation failure throws ValidationException'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(
                TransactionBuilder.builder().withGuid('invalid').build())

        when:
        transactionService.updateTransaction(transaction)

        then:
        thrown(ValidationException)
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test fetchTotalsByAccountNameOwner - calculates cleared and totals'() {
        given:
        String accountNameOwner = 'chase_brian'
        Transaction cleared = TransactionBuilder.builder()
                .withAmount(new BigDecimal('10.00'))
                .withTransactionState(TransactionState.Cleared)
                .build()
        Transaction outstanding = TransactionBuilder.builder()
                .withAmount(new BigDecimal('5.00'))
                .withTransactionState(TransactionState.Outstanding)
                .build()

        when:
        Map<String, BigDecimal> result = transactionService.fetchTotalsByAccountNameOwner(accountNameOwner)

        then:
        result['totals'] == new BigDecimal('15.00')
        result['totalsCleared'] == new BigDecimal('10.00')
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> [cleared, outstanding]
        0 * _
    }

    void 'test fetchTotalsByAccountNameOwner - empty account returns zeros'() {
        given:
        String accountNameOwner = 'empty_brian'

        when:
        Map<String, BigDecimal> result = transactionService.fetchTotalsByAccountNameOwner(accountNameOwner)

        then:
        result['totals'] == new BigDecimal('0.00')
        result['totalsCleared'] == new BigDecimal('0.00')
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> []
        0 * _
    }

    void 'test findByAccountNameOwnerOrderByTransactionDate - returns sorted transactions'() {
        given:
        String accountNameOwner = 'chase_brian'
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        List<Transaction> results = transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)

        then:
        results.size() == 1
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> [transaction]
        0 * _
    }

    void 'test findByAccountNameOwnerOrderByTransactionDate - empty list logs warning and increments meter'() {
        given:
        String accountNameOwner = 'nonexistent_brian'

        when:
        List<Transaction> results = transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)

        then:
        results.isEmpty()
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> []
        1 * meterRegistryMock.counter(setMeterId(finance.utils.Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER, 'non-existent-accounts')) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test findAllActiveTransactions - returns sorted list'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        List<Transaction> results = transactionService.findAllActiveTransactions()

        then:
        results.size() == 1
        1 * transactionRepositoryMock.findByActiveStatusOrderByTransactionDateDesc(true) >> [transaction]
        0 * _
    }

    void 'test findTransactionsByCategory - returns transactions'() {
        given:
        String categoryName = 'online'
        Transaction transaction = TransactionBuilder.builder().withCategory(categoryName).build()

        when:
        List<Transaction> results = transactionService.findTransactionsByCategory(categoryName)

        then:
        results.size() == 1
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName, true) >> [transaction]
        0 * _
    }

    void 'test findTransactionsByDescription - returns transactions'() {
        given:
        String descriptionName = 'aliexpress.com'
        Transaction transaction = TransactionBuilder.builder().withDescription(descriptionName).build()

        when:
        List<Transaction> results = transactionService.findTransactionsByDescription(descriptionName)

        then:
        results.size() == 1
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName, true) >> [transaction]
        0 * _
    }

    void 'test findTransactionsByDateRange - returns transactions within range'() {
        given:
        LocalDate startDate = LocalDate.of(2020, 1, 1)
        LocalDate endDate = LocalDate.of(2020, 12, 31)
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        List<Transaction> results = transactionService.findTransactionsByDateRange(startDate, endDate)

        then:
        results.size() == 1
        1 * transactionRepositoryMock.findByTransactionDateBetweenAndActiveStatusOrderByTransactionDateDesc(startDate, endDate, true) >> [transaction]
        0 * _
    }

    void 'test updateTransactionState - future dated transaction throws RuntimeException'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder()
                .withGuid(guid)
                .withTransactionDate(LocalDate.now().plusDays(5))
                .build()

        when:
        transactionService.updateTransactionState(guid, TransactionState.Cleared)

        then:
        thrown(RuntimeException)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test updateTransactionState - guid not found throws RuntimeException'() {
        given:
        String guid = UUID.randomUUID()

        when:
        transactionService.updateTransactionState(guid, TransactionState.Cleared)

        then:
        thrown(RuntimeException)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test updateTransactionReoccurringFlag - guid not found throws RuntimeException'() {
        given:
        String guid = UUID.randomUUID()

        when:
        transactionService.updateTransactionReoccurringFlag(guid, true)

        then:
        thrown(RuntimeException)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test changeAccountNameOwner - success'() {
        given:
        String guid = UUID.randomUUID()
        String accountNameOwner = 'chase_brian'
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        account.accountNameOwner = accountNameOwner
        account.accountId = 1L

        when:
        Boolean result = transactionService.changeAccountNameOwner([accountNameOwner: accountNameOwner, guid: guid])

        then:
        result
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        0 * _
    }

    void 'test changeAccountNameOwner - null guid throws RuntimeException'() {
        when:
        transactionService.changeAccountNameOwner([accountNameOwner: 'chase_brian', guid: null])

        then:
        thrown(RuntimeException)
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test changeAccountNameOwner - transaction not found throws RuntimeException'() {
        given:
        String guid = UUID.randomUUID()
        String accountNameOwner = 'chase_brian'
        Account account = new Account()
        account.accountNameOwner = accountNameOwner

        when:
        transactionService.changeAccountNameOwner([accountNameOwner: accountNameOwner, guid: guid])

        then:
        thrown(RuntimeException)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        0 * _
    }

    void 'test insertTransaction - validation failure throws ValidationException'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(
                TransactionBuilder.builder().withGuid('invalid').build())

        when:
        transactionService.insertTransaction(transaction)

        then:
        thrown(ValidationException)
        1 * validatorMock.validate(_ as Transaction) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test createFutureTransaction - FortNightly adds 14 days'() {
        given:
        Transaction transaction = TransactionBuilder.builder()
                .withTransactionDate(LocalDate.of(2020, 6, 1))
                .withReoccurringType(ReoccurringType.FortNightly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(transaction)

        then:
        result.transactionDate == LocalDate.of(2020, 6, 15)
        result.transactionState == TransactionState.Future
        result.notes == ''
        0 * _
    }

    void 'test masterTransactionUpdater - cleared reoccurring creates future transaction'() {
        given:
        String guid = UUID.randomUUID()
        Transaction dbTransaction = TransactionBuilder.builder()
                .withGuid(guid)
                .withTransactionDate(LocalDate.of(2020, 1, 15))
                .withTransactionState(TransactionState.Cleared)
                .withReoccurringType(ReoccurringType.Monthly)
                .build()
        Transaction incomingTransaction = TransactionBuilder.builder()
                .withGuid(guid)
                .withTransactionDate(LocalDate.of(2020, 1, 15))
                .withTransactionState(TransactionState.Cleared)
                .withReoccurringType(ReoccurringType.Monthly)
                .build()
        Category cat = new Category()
        cat.categoryName = incomingTransaction.category
        Description desc = new Description()
        desc.descriptionName = incomingTransaction.description

        when:
        Boolean result = transactionService.masterTransactionUpdater(dbTransaction, incomingTransaction)

        then:
        result
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(_, incomingTransaction.category) >> Optional.of(cat)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(_, incomingTransaction.description) >> Optional.of(desc)
        1 * transactionRepositoryMock.update(_ as Transaction)
        1 * transactionRepositoryMock.saveAndFlush({ Transaction future ->
            assert future.transactionState == TransactionState.Future
            future
        }) >> incomingTransaction
        0 * _
    }

    void 'test deleteReceiptImageForTransactionByGuid - transaction not found throws RuntimeException'() {
        given:
        String guid = UUID.randomUUID()

        when:
        transactionService.deleteReceiptImageForTransactionByGuid(guid)

        then:
        thrown(RuntimeException)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        0 * _
    }

    void 'test deleteReceiptImageForTransactionByGuid - no receipt image returns false'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()

        when:
        Boolean result = transactionService.deleteReceiptImageForTransactionByGuid(guid)

        then:
        !result
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        0 * _
    }
}
