package finance.services

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date

class PendingTransactionServiceSpec extends Specification {

    private PendingTransactionRepository pendingTransactionRepositoryMock = GroovyMock(PendingTransactionRepository)
    private PendingTransactionService service = new PendingTransactionService(pendingTransactionRepositoryMock)

    private PendingTransaction buildPendingTransaction() {
        PendingTransaction pt = new PendingTransaction()
        pt.pendingTransactionId = 1L
        pt.accountNameOwner = 'chase_brian'
        pt.description = 'amazon.com'
        pt.amount = new BigDecimal('25.00')
        pt.transactionDate = Date.valueOf('2020-01-15')
        return pt
    }

    void 'test insertPendingTransaction - saves and returns transaction'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        PendingTransaction result = service.insertPendingTransaction(pt)

        then:
        result == pt
        1 * pendingTransactionRepositoryMock.saveAndFlush(pt) >> pt
        0 * _
    }

    void 'test getAllPendingTransactions - returns all transactions'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        List<PendingTransaction> results = service.getAllPendingTransactions()

        then:
        results.size() == 1
        1 * pendingTransactionRepositoryMock.findAll() >> [pt]
        0 * _
    }

    void 'test getAllPendingTransactions - empty list'() {
        when:
        List<PendingTransaction> results = service.getAllPendingTransactions()

        then:
        results.isEmpty()
        1 * pendingTransactionRepositoryMock.findAll() >> []
        0 * _
    }

    void 'test deletePendingTransaction - finds and deletes'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        Boolean result = service.deletePendingTransaction(1L)

        then:
        result
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(1L) >> Optional.of(pt)
        1 * pendingTransactionRepositoryMock.delete(pt)
        0 * _
    }

    void 'test deleteAllPendingTransactions - deletes all'() {
        when:
        Boolean result = service.deleteAllPendingTransactions()

        then:
        result
        1 * pendingTransactionRepositoryMock.deleteAll()
        0 * _
    }

    void 'test findByPendingTransactionId - found'() {
        given:
        PendingTransaction pt = buildPendingTransaction()

        when:
        Optional<PendingTransaction> result = service.findByPendingTransactionId(1L)

        then:
        result.isPresent()
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(1L) >> Optional.of(pt)
        0 * _
    }

    void 'test findByPendingTransactionId - not found returns empty'() {
        when:
        Optional<PendingTransaction> result = service.findByPendingTransactionId(999L)

        then:
        !result.isPresent()
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(999L) >> Optional.empty()
        0 * _
    }

    void 'test updatePendingTransaction - found and updated'() {
        given:
        PendingTransaction existing = buildPendingTransaction()
        PendingTransaction updated = buildPendingTransaction()
        updated.amount = new BigDecimal('50.00')
        updated.description = 'bestbuy.com'

        when:
        Optional<PendingTransaction> result = service.updatePendingTransaction(1L, updated)

        then:
        result.isPresent()
        result.get().amount == new BigDecimal('50.00')
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(1L) >> Optional.of(existing)
        1 * pendingTransactionRepositoryMock.saveAndFlush(existing) >> existing
        0 * _
    }

    void 'test updatePendingTransaction - not found returns empty'() {
        given:
        PendingTransaction updated = buildPendingTransaction()

        when:
        Optional<PendingTransaction> result = service.updatePendingTransaction(999L, updated)

        then:
        !result.isPresent()
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(999L) >> Optional.empty()
        0 * _
    }
}
