package finance.services

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import finance.domain.Account
import finance.domain.AccountType
import finance.helpers.AccountBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import java.math.BigDecimal
import java.math.RoundingMode

@SuppressWarnings("GroovyAccessibility")
class AccountServiceSpec extends BaseServiceSpec {
    protected AccountService accountService = new AccountService(accountRepositoryMock, transactionRepositoryMock, validatorMock, meterService)

    protected String validJsonPayload = '''
{
"accountNameOwner": "test_brian",
"accountType": "credit",
"activeStatus": "true",
"moniker": "0000",
"totals": 0.00,
"totalsBalanced": 0.00,
"dateClosed": 0
}
'''

    void 'test findAllActiveAccounts empty'() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = []
        accounts.add(account)

        when:
        List<Account> results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    void 'test findAllActiveAccounts'() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = [account, account, account, account]

        when:
        List<Account> results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 4
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    void 'test insertAccount - attempt to insert a preexisting account'() {
        given:
        Account account = AccountBuilder.builder().build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        !isInserted
        constraintViolations.size() == 0
        1 * validatorMock.validate(account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }

    void 'test insertAccount - attempt to insert a empty accountNameOwner'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('').build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        thrown(ValidationException)
        constraintViolations.size() >= 1
        1 * validatorMock.validate(_ as Account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertAccount - json inserted success'() {
        given:
        Account account = mapper.readValue(validJsonPayload, Account)
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        isInserted
        1 * validatorMock.validate(account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.saveAndFlush(account)
        0 * _
    }

    void 'test insertAccount - invalid accountNameOwner too short'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('ab').build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        constraintViolations.size() == 1
        ValidationException ex = thrown(ValidationException)
        ex.message.contains('Cannot insert account as there is a constraint violation')
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * validatorMock.validate(_ as Account) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertAccount - bad json - accountType'() {
        given:
        String jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"Credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0}"

        when:
        mapper.readValue(jsonPayload, Account)

        then:
        InvalidFormatException ex = thrown()
        ex.message.contains('not one of the values accepted for Enum class')
        0 * _
    }

    void 'computeTheGrandTotalForAllTransactions'() {
        given:
        BigDecimal desiredResult = new BigDecimal(5.75).setScale(2, RoundingMode.HALF_UP)
        when:
        BigDecimal result = accountService.computeTheGrandTotalForAllTransactions()

        then:
        result == desiredResult
        1 * accountRepositoryMock.computeTheGrandTotalForAllTransactions() >> desiredResult
        0 * _
    }

    void 'computeTheGrandTotalForAllClearedTransactions'() {
        given:
        BigDecimal desiredResult = new BigDecimal(8.92).setScale(2, RoundingMode.HALF_UP)
        when:
        BigDecimal result = accountService.computeTheGrandTotalForAllClearedTransactions()

        then:
        result == desiredResult
        1 * accountRepositoryMock.computeTheGrandTotalForAllClearedTransactions() >> desiredResult
        0 * _
    }

    void 'computeTheGrandTotalForAllClearedTransactions - 3 decimal'() {
        given:
        BigDecimal desiredResult = new BigDecimal(8.923).setScale(3, RoundingMode.HALF_UP)
        when:
        BigDecimal result = accountService.computeTheGrandTotalForAllClearedTransactions()

        then:
        result != desiredResult
        1 * accountRepositoryMock.computeTheGrandTotalForAllClearedTransactions() >> desiredResult
        0 * _
    }

    void 'test deleteByAccountNameOwner - returns true'() {
        given:
        String accountNameOwner = 'foo_brian'

        when:
        Boolean result = accountService.deleteByAccountNameOwner(accountNameOwner)

        then:
        result
        1 * accountRepositoryMock.deleteByAccountNameOwner(accountNameOwner)
        0 * _
    }

    void 'test updateAccount - account found and saved'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        Boolean result = accountService.updateAccount(account)

        then:
        result
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * accountRepositoryMock.saveAndFlush(account)
        0 * _
    }

    void 'test updateAccount - account not found returns false'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        Boolean result = accountService.updateAccount(account)

        then:
        !result
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        0 * _
    }

    void 'test renameAccountNameOwner - success'() {
        given:
        Account oldAccount = AccountBuilder.builder().withAccountNameOwner('old_brian').build()
        Account newAccount = AccountBuilder.builder().withAccountNameOwner('new_brian').build()

        when:
        Boolean result = accountService.renameAccountNameOwner('old_brian', 'new_brian')

        then:
        result
        1 * accountRepositoryMock.findByAccountNameOwner('new_brian') >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner('old_brian') >> Optional.of(oldAccount)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> newAccount
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc('old_brian', true) >> []
        1 * accountRepositoryMock.deleteByAccountNameOwner('old_brian')
        0 * _
    }

    void 'test renameAccountNameOwner - old account not found throws RuntimeException'() {
        when:
        accountService.renameAccountNameOwner('nonexistent_brian', 'new_brian')

        then:
        thrown(RuntimeException)
        1 * accountRepositoryMock.findByAccountNameOwner('new_brian') >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner('nonexistent_brian') >> Optional.empty()
        0 * _
    }

    void 'test renameAccountNameOwner - new account already exists throws RuntimeException'() {
        given:
        Account existing = AccountBuilder.builder().withAccountNameOwner('existing_brian').build()
        Account old = AccountBuilder.builder().withAccountNameOwner('old_brian').build()

        when:
        accountService.renameAccountNameOwner('old_brian', 'existing_brian')

        then:
        thrown(RuntimeException)
        1 * accountRepositoryMock.findByAccountNameOwner('existing_brian') >> Optional.of(existing)
        1 * accountRepositoryMock.findByAccountNameOwner('old_brian') >> Optional.of(old)
        0 * _
    }

    void 'test deactivateAccount - success'() {
        given:
        Account account = AccountBuilder.builder().withActiveStatus(true).build()

        when:
        Account result = accountService.deactivateAccount(account.accountNameOwner)

        then:
        !result.activeStatus
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * accountRepositoryMock.saveAndFlush(account) >> account
        0 * _
    }

    void 'test deactivateAccount - not found throws RuntimeException'() {
        when:
        accountService.deactivateAccount('nonexistent_brian')

        then:
        thrown(RuntimeException)
        1 * accountRepositoryMock.findByAccountNameOwner('nonexistent_brian') >> Optional.empty()
        0 * _
    }

    void 'test activateAccount - success'() {
        given:
        Account account = AccountBuilder.builder().withActiveStatus(false).build()

        when:
        Account result = accountService.activateAccount(account.accountNameOwner)

        then:
        result.activeStatus
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.reactivateAllTransactionsByAccountNameOwner(account.accountNameOwner) >> 0
        1 * accountRepositoryMock.saveAndFlush(account) >> account
        0 * _
    }

    void 'test activateAccount - not found throws RuntimeException'() {
        when:
        accountService.activateAccount('nonexistent_brian')

        then:
        thrown(RuntimeException)
        1 * accountRepositoryMock.findByAccountNameOwner('nonexistent_brian') >> Optional.empty()
        0 * _
    }

    void 'test updateTotalsForAllAccounts - success returns true'() {
        when:
        Boolean result = accountService.updateTotalsForAllAccounts()

        then:
        result
        1 * accountRepositoryMock.updateTotalsForAllAccounts()
        0 * _
    }

    void 'test findAccountsThatRequirePayment - returns list of accounts needing payment'() {
        when:
        List<String> results = accountService.findAccountsThatRequirePayment()

        then:
        results != null
        1 * accountRepositoryMock.findAccountsThatRequirePayment() >> []
        0 * _
    }

    void 'test findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner - returns accounts'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        List<Account> results = accountService.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * accountRepositoryMock.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(true, AccountType.Credit, new BigDecimal(0.0)) >> [account]
        0 * _
    }

    void 'test findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner - empty list'() {
        when:
        List<Account> results = accountService.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner()

        then:
        results.isEmpty()
        1 * accountRepositoryMock.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(true, AccountType.Credit, new BigDecimal(0.0)) >> []
        0 * _
    }

    void 'test account - delegates to findByAccountNameOwner'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        Optional<Account> result = accountService.account(account.accountNameOwner)

        then:
        result.isPresent()
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }
}