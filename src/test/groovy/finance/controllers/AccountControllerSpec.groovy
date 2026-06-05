package finance.controllers

import finance.domain.Account
import finance.helpers.AccountBuilder
import finance.services.AccountService
import finance.services.OwnerExtractorService
import finance.services.TransactionService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class AccountControllerSpec extends Specification {

    private AccountService accountServiceMock = GroovyMock(AccountService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private TransactionService transactionServiceMock = GroovyMock(TransactionService)
    private AccountController controller = new AccountController(accountServiceMock, ownerExtractorServiceMock, transactionServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    void 'test selectAllActiveAccounts - returns 200 with accounts'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllActiveAccounts()

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.updateTheGrandTotalForAllClearedTransactions()
        1 * accountServiceMock.findByActiveStatusOrderByAccountNameOwner() >> [account]
        0 * _
    }

    void 'test selectAllActiveAccounts - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActiveAccounts()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * accountServiceMock.updateTheGrandTotalForAllClearedTransactions()
        1 * accountServiceMock.findByActiveStatusOrderByAccountNameOwner() >> []
        0 * _
    }

    void 'test selectByAccountNameOwner - returns 200 when found'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.selectByAccountNameOwner('foo_brian')

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.findByAccountNameOwner('foo_brian') >> Optional.of(account)
        0 * _
    }

    void 'test selectByAccountNameOwner - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectByAccountNameOwner('notfound_brian')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * accountServiceMock.findByAccountNameOwner('notfound_brian') >> Optional.empty()
        0 * _
    }

    void 'test insertAccount - returns 401 when no owner'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.insertAccount(account, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertAccount - returns 201 on success'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.insertAccount(account, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * accountServiceMock.insertAccount(account)
        0 * _
    }

    void 'test updateAccount - returns 401 when no owner'() {
        given:
        Map<String, Object> accountMap = [accountNameOwner: 'foo_brian', accountType: 'credit', moniker: '1234', activeStatus: true]

        when:
        HttpResponse response = controller.updateAccount('foo_brian', accountMap, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updateAccount - returns 200 on success'() {
        given:
        Map<String, Object> accountMap = [accountNameOwner: 'foo_brian', accountType: 'credit', moniker: '1234', activeStatus: true, outstanding: 0.0, cleared: 0.0, future: 0.0]

        when:
        HttpResponse response = controller.updateAccount('foo_brian', accountMap, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * accountServiceMock.updateAccount(_ as Account) >> true
        0 * _
    }

    void 'test updateAccount - returns 400 when service returns false'() {
        given:
        Map<String, Object> accountMap = [accountNameOwner: 'foo_brian', accountType: 'credit', moniker: '1234', activeStatus: true, outstanding: 0.0, cleared: 0.0, future: 0.0]

        when:
        HttpResponse response = controller.updateAccount('foo_brian', accountMap, requestMock)

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * accountServiceMock.updateAccount(_ as Account) >> false
        0 * _
    }

    void 'test deleteByAccountNameOwner - returns 200 when found'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByAccountNameOwner('foo_brian')

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.findByAccountNameOwner('foo_brian') >> Optional.of(account)
        1 * accountServiceMock.deleteByAccountNameOwner('foo_brian')
        0 * _
    }

    void 'test deleteByAccountNameOwner - returns 400 when not found'() {
        when:
        HttpResponse response = controller.deleteByAccountNameOwner('notfound_brian')

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * accountServiceMock.findByAccountNameOwner('notfound_brian') >> Optional.empty()
        0 * _
    }

    void 'test computeAccountTotals - returns map with totals'() {
        when:
        Map<String, String> result = controller.computeAccountTotals()

        then:
        result.containsKey('totals')
        result.containsKey('totalsCleared')
        1 * accountServiceMock.computeTheGrandTotalForAllTransactions() >> new BigDecimal('100.00')
        1 * accountServiceMock.computeTheGrandTotalForAllClearedTransactions() >> new BigDecimal('50.00')
        0 * _
    }

    void 'test refreshValidationDates - returns 200'() {
        when:
        HttpResponse response = controller.refreshValidationDates()

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.refreshValidationDates()
        0 * _
    }

    void 'test selectPaymentRequired - returns 200 with accounts'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        HttpResponse response = controller.selectPaymentRequired()

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findAccountsThatRequirePayment() >> [account]
        0 * _
    }

    void 'test selectPaymentRequired - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectPaymentRequired()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findAccountsThatRequirePayment() >> []
        0 * _
    }

    void 'test renameAccountNameOwner - returns 200 on success'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('new_brian').build()

        when:
        HttpResponse response = controller.renameAccountNameOwner('old_brian', 'new_brian')

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.renameAccountNameOwner('old_brian', 'new_brian') >> true
        1 * accountServiceMock.findByAccountNameOwner('new_brian') >> Optional.of(account)
        0 * _
    }

    void 'test renameAccountNameOwner - returns 400 when service returns false'() {
        when:
        HttpResponse response = controller.renameAccountNameOwner('old_brian', 'new_brian')

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * accountServiceMock.renameAccountNameOwner('old_brian', 'new_brian') >> false
        0 * _
    }

    void 'test deactivateAccount - returns 200 on success'() {
        given:
        Account account = AccountBuilder.builder().withActiveStatus(false).build()

        when:
        HttpResponse response = controller.deactivateAccount('foo_brian')

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.deactivateAccount('foo_brian') >> account
        0 * _
    }

    void 'test deactivateAccount - returns 404 on RuntimeException'() {
        when:
        HttpResponse response = controller.deactivateAccount('notfound_brian')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * accountServiceMock.deactivateAccount('notfound_brian') >> { throw new RuntimeException('not found') }
        0 * _
    }

    void 'test activateAccount - returns 200 on success'() {
        given:
        Account account = AccountBuilder.builder().withActiveStatus(true).build()

        when:
        HttpResponse response = controller.activateAccount('foo_brian')

        then:
        response.status == HttpStatus.OK
        1 * accountServiceMock.activateAccount('foo_brian') >> account
        0 * _
    }

    void 'test activateAccount - returns 404 on RuntimeException'() {
        when:
        HttpResponse response = controller.activateAccount('notfound_brian')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * accountServiceMock.activateAccount('notfound_brian') >> { throw new RuntimeException('not found') }
        0 * _
    }
}
