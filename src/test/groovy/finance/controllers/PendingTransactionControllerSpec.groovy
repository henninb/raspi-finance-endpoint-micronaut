package finance.controllers

import finance.domain.PendingTransaction
import finance.services.OwnerExtractorService
import finance.services.PendingTransactionService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date

@SuppressWarnings("GroovyAccessibility")
class PendingTransactionControllerSpec extends Specification {

    private PendingTransactionService pendingTransactionServiceMock = GroovyMock(PendingTransactionService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private PendingTransactionController controller = new PendingTransactionController(pendingTransactionServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    private PendingTransaction buildPendingTransaction() {
        PendingTransaction pt = new PendingTransaction()
        pt.pendingTransactionId = 1L
        pt.accountNameOwner = 'chase_brian'
        pt.description = 'amazon.com'
        pt.amount = new BigDecimal('25.00')
        pt.transactionDate = Date.valueOf('2020-01-15')
        return pt
    }

    void 'test getAllPendingTransactions - returns 200 with list'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.getAllPendingTransactions()

        then:
        response.status == HttpStatus.OK
        1 * pendingTransactionServiceMock.getAllPendingTransactions() >> [pt]
        0 * _
    }

    void 'test getAllPendingTransactions - throws 404 when empty'() {
        when:
        controller.getAllPendingTransactions()

        then:
        thrown(HttpStatusException)
        1 * pendingTransactionServiceMock.getAllPendingTransactions() >> []
        0 * _
    }

    void 'test selectByPendingTransactionId - returns 200 when found'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.selectByPendingTransactionId(1L)

        then:
        response.status == HttpStatus.OK
        1 * pendingTransactionServiceMock.findByPendingTransactionId(1L) >> Optional.of(pt)
        0 * _
    }

    void 'test selectByPendingTransactionId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectByPendingTransactionId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * pendingTransactionServiceMock.findByPendingTransactionId(999L) >> Optional.empty()
        0 * _
    }

    void 'test insertPendingTransaction - returns 401 when no owner'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.insertPendingTransaction(pt, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertPendingTransaction - returns 201 on success'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.insertPendingTransaction(pt, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * pendingTransactionServiceMock.insertPendingTransaction(pt) >> pt
        0 * _
    }

    void 'test updatePendingTransaction - returns 401 when no owner'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.updatePendingTransaction(1L, pt, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updatePendingTransaction - returns 200 on success'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        HttpResponse response = controller.updatePendingTransaction(1L, pt, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * pendingTransactionServiceMock.updatePendingTransaction(1L, pt) >> Optional.of(pt)
        0 * _
    }

    void 'test updatePendingTransaction - throws 404 when not found'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        controller.updatePendingTransaction(999L, pt, requestMock)

        then:
        thrown(HttpStatusException)
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * pendingTransactionServiceMock.updatePendingTransaction(999L, pt) >> Optional.empty()
        0 * _
    }

    void 'test deletePendingTransaction - returns 204 on success'() {
        when:
        HttpResponse response = controller.deletePendingTransaction(1L)

        then:
        response.status == HttpStatus.NO_CONTENT
        1 * pendingTransactionServiceMock.deletePendingTransaction(1L) >> true
        0 * _
    }

    void 'test deletePendingTransaction - throws 400 when fails'() {
        when:
        controller.deletePendingTransaction(999L)

        then:
        thrown(HttpStatusException)
        1 * pendingTransactionServiceMock.deletePendingTransaction(999L) >> false
        0 * _
    }

    void 'test deleteAllPendingTransactions - returns 204 on success'() {
        when:
        HttpResponse response = controller.deleteAllPendingTransactions()

        then:
        response.status == HttpStatus.NO_CONTENT
        1 * pendingTransactionServiceMock.deleteAllPendingTransactions() >> true
        0 * _
    }
}
