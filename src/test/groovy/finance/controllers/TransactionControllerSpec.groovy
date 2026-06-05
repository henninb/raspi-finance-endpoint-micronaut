package finance.controllers

import finance.domain.Account
import finance.domain.BonusProgress
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.AccountBuilder
import finance.helpers.ReceiptImageBuilder
import finance.helpers.TransactionBuilder
import finance.services.OwnerExtractorService
import finance.services.TransactionService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDate

@SuppressWarnings("GroovyAccessibility")
class TransactionControllerSpec extends Specification {

    private TransactionService transactionServiceMock = GroovyMock(TransactionService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private TransactionController controller = new TransactionController(transactionServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    void 'test selectByAccountNameOwner - returns 200 with transactions'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectByAccountNameOwner('chase_brian')

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate('chase_brian') >> [transaction]
        0 * _
    }

    void 'test selectByAccountNameOwner - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectByAccountNameOwner('empty_brian')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate('empty_brian') >> []
        0 * _
    }

    void 'test selectTotalsCleared - returns 200 with totals map'() {
        given:
        Map<String, BigDecimal> totals = [totals: new BigDecimal('100.00'), totalsCleared: new BigDecimal('50.00')]

        when:
        HttpResponse response = controller.selectTotalsCleared('chase_brian')

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.fetchTotalsByAccountNameOwner('chase_brian') >> totals
        0 * _
    }

    void 'test findTransaction - returns 200 when found'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.findTransaction(guid)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findTransactionByGuid(guid) >> Optional.of(transaction)
        0 * _
    }

    void 'test findTransaction - returns 404 when not found'() {
        given:
        String guid = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'

        when:
        HttpResponse response = controller.findTransaction(guid)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findTransactionByGuid(guid) >> Optional.empty()
        0 * _
    }

    private Map<String, Object> buildTransactionMap(String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd') {
        return [
            transactionId: 0L,
            guid: guid,
            accountId: 0L,
            accountType: 'credit',
            accountNameOwner: 'chase_brian',
            transactionDate: '2020-12-01',
            description: 'amazon.com',
            category: 'online',
            amount: 3.14,
            transactionState: 'cleared',
            reoccurringType: 'undefined',
            activeStatus: true,
            notes: 'note'
        ]
    }

    void 'test updateTransaction - returns 200 on success'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
        Map<String, Object> payload = buildTransactionMap(guid)

        when:
        HttpResponse response = controller.updateTransaction(guid, payload, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * transactionServiceMock.updateTransaction(_ as Transaction) >> true
        0 * _
    }

    void 'test updateTransaction - returns 404 when service returns false'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
        Map<String, Object> payload = buildTransactionMap(guid)

        when:
        HttpResponse response = controller.updateTransaction(guid, payload, requestMock)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * transactionServiceMock.updateTransaction(_ as Transaction) >> false
        0 * _
    }

    void 'test updateTransactionState - returns 200 when transactions returned'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.updateTransactionState(guid, TransactionState.Cleared)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.updateTransactionState(guid, TransactionState.Cleared) >> [transaction]
        0 * _
    }

    void 'test updateTransactionState - returns 304 when empty'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.updateTransactionState(guid, TransactionState.Cleared)

        then:
        response.status == HttpStatus.NOT_MODIFIED
        1 * transactionServiceMock.updateTransactionState(guid, TransactionState.Cleared) >> []
        0 * _
    }

    void 'test updateTransactionReoccurringState - returns 200 on success'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.updateTransactionReoccurringState(guid, true)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.updateTransactionReoccurringFlag(guid, true) >> true
        0 * _
    }

    void 'test updateTransactionReoccurringState - returns 304 when fails'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.updateTransactionReoccurringState(guid, false)

        then:
        response.status == HttpStatus.NOT_MODIFIED
        1 * transactionServiceMock.updateTransactionReoccurringFlag(guid, false) >> false
        0 * _
    }

    void 'test insertTransaction - returns 401 when no owner'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.insertTransaction(transaction, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertTransaction - returns 201 on success'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.insertTransaction(transaction, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * transactionServiceMock.insertTransaction(transaction) >> true
        0 * _
    }

    void 'test insertTransaction - returns 400 when service returns false'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.insertTransaction(transaction, requestMock)

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * transactionServiceMock.insertTransaction(transaction) >> false
        0 * _
    }

    void 'test changeTransactionAccountNameOwner - returns 200'() {
        given:
        Map<String, String> payload = [guid: '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd', accountNameOwner: 'new_brian']

        when:
        HttpResponse response = controller.changeTransactionAccountNameOwner(payload)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.changeAccountNameOwner(payload)
        0 * _
    }

    void 'test updateTransactionReceiptImageByGuid - returns 200'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.updateTransactionReceiptImageByGuid(guid, 'base64imagedata')

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.updateTransactionReceiptImageByGuid(guid, 'base64imagedata') >> receiptImage
        0 * _
    }

    void 'test deleteTransaction - returns 200 when found and deleted'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.deleteTransaction(guid)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findTransactionByGuid(guid) >> Optional.of(transaction)
        1 * transactionServiceMock.deleteTransactionByGuid(guid) >> true
        0 * _
    }

    void 'test deleteTransaction - returns 404 when not found'() {
        given:
        String guid = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'

        when:
        HttpResponse response = controller.deleteTransaction(guid)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findTransactionByGuid(guid) >> Optional.empty()
        0 * _
    }

    void 'test selectAllActiveTransactions - returns 200 with transactions'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllActiveTransactions()

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findAllActiveTransactions() >> [transaction]
        0 * _
    }

    void 'test selectAllActiveTransactions - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActiveTransactions()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findAllActiveTransactions() >> []
        0 * _
    }

    void 'test selectTransactionsByCategory - returns 200 with transactions'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectTransactionsByCategory('online')

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findTransactionsByCategory('online') >> [transaction]
        0 * _
    }

    void 'test selectTransactionsByCategory - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectTransactionsByCategory('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findTransactionsByCategory('notfound') >> []
        0 * _
    }

    void 'test selectTransactionsByDescription - returns 200 with transactions'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectTransactionsByDescription('amazon.com')

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findTransactionsByDescription('amazon.com') >> [transaction]
        0 * _
    }

    void 'test selectTransactionsByDescription - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectTransactionsByDescription('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findTransactionsByDescription('notfound') >> []
        0 * _
    }

    void 'test selectTransactionsByDateRange - returns 200 with transactions'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        LocalDate start = LocalDate.of(2020, 1, 1)
        LocalDate end = LocalDate.of(2020, 12, 31)

        when:
        HttpResponse response = controller.selectTransactionsByDateRange(start, end)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findTransactionsByDateRange(start, end) >> [transaction]
        0 * _
    }

    void 'test selectTransactionsByDateRange - returns 404 when empty'() {
        given:
        LocalDate start = LocalDate.of(2019, 1, 1)
        LocalDate end = LocalDate.of(2019, 12, 31)

        when:
        HttpResponse response = controller.selectTransactionsByDateRange(start, end)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.findTransactionsByDateRange(start, end) >> []
        0 * _
    }

    void 'test selectByAccountNameOwnerPaged - returns 200 with paged results'() {
        given:
        Page<Transaction> page = Mock(Page)
        Pageable pageable = Mock(Pageable)

        when:
        HttpResponse response = controller.selectByAccountNameOwnerPaged('chase_brian', pageable)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.findByAccountNameOwnerPaged('chase_brian', pageable) >> page
        0 * _
    }

    void 'test getBonusProgress - returns 200'() {
        given:
        BonusProgress bonusProgress = new BonusProgress('chase_brian',
            new BigDecimal('500.00'), new BigDecimal('0.00'), new BigDecimal('1000.00'),
            new BigDecimal('500.00'), 50.0d, new BigDecimal('100.00'), false,
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 4, 1), 30L)
        LocalDate startDate = LocalDate.of(2020, 1, 1)
        BigDecimal targetAmount = new BigDecimal('1000.00')
        BigDecimal bonusAmount = new BigDecimal('100.00')

        when:
        HttpResponse response = controller.getBonusProgress('chase_brian', startDate, targetAmount, bonusAmount, 90L)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.getBonusProgress('chase_brian', startDate, targetAmount, bonusAmount, 90L) >> bonusProgress
        0 * _
    }

    void 'test insertFutureTransaction - returns 200'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        HttpResponse response = controller.insertFutureTransaction(transaction)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.insertFutureTransaction(transaction) >> transaction
        0 * _
    }

    void 'test deleteTransactionReceiptImageByGuid - returns 200 when deleted'() {
        given:
        String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'

        when:
        HttpResponse response = controller.deleteTransactionReceiptImageByGuid(guid)

        then:
        response.status == HttpStatus.OK
        1 * transactionServiceMock.deleteReceiptImageForTransactionByGuid(guid) >> true
        0 * _
    }

    void 'test deleteTransactionReceiptImageByGuid - returns 404 when not found'() {
        given:
        String guid = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'

        when:
        HttpResponse response = controller.deleteTransactionReceiptImageByGuid(guid)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transactionServiceMock.deleteReceiptImageForTransactionByGuid(guid) >> false
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
}
