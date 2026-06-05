package finance.services

import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.Totals
import finance.helpers.TransactionBuilder

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@SuppressWarnings("GroovyAccessibility")
class CalculationServiceSpec extends BaseServiceSpec {

    void 'calculateTotalsFromTransactions - empty list returns empty map'() {
        when:
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions([])

        then:
        result.isEmpty()
        0 * _
    }

    void 'calculateTotalsFromTransactions - single cleared transaction'() {
        given:
        Transaction t = TransactionBuilder.builder()
                .withAmount(new BigDecimal('10.00'))
                .withTransactionState(TransactionState.Cleared)
                .build()

        when:
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions([t])

        then:
        result[TransactionState.Cleared] == new BigDecimal('10.00')
        result.size() == 1
        0 * _
    }

    void 'calculateTotalsFromTransactions - multiple states accumulate correctly'() {
        given:
        Transaction cleared1 = TransactionBuilder.builder()
                .withAmount(new BigDecimal('5.00'))
                .withTransactionState(TransactionState.Cleared)
                .build()
        Transaction cleared2 = TransactionBuilder.builder()
                .withAmount(new BigDecimal('3.00'))
                .withTransactionState(TransactionState.Cleared)
                .build()
        Transaction outstanding = TransactionBuilder.builder()
                .withAmount(new BigDecimal('7.50'))
                .withTransactionState(TransactionState.Outstanding)
                .build()
        Transaction future = TransactionBuilder.builder()
                .withAmount(new BigDecimal('2.25'))
                .withTransactionState(TransactionState.Future)
                .build()

        when:
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions([cleared1, cleared2, outstanding, future])

        then:
        result[TransactionState.Cleared] == new BigDecimal('8.00')
        result[TransactionState.Outstanding] == new BigDecimal('7.50')
        result[TransactionState.Future] == new BigDecimal('2.25')
        result.size() == 3
        0 * _
    }

    void 'calculateGrandTotal - returns sum of all values'() {
        given:
        Map<TransactionState, BigDecimal> totalsMap = [
                (TransactionState.Cleared)    : new BigDecimal('10.00'),
                (TransactionState.Outstanding): new BigDecimal('5.00'),
                (TransactionState.Future)     : new BigDecimal('3.00'),
        ]

        when:
        BigDecimal result = calculationService.calculateGrandTotal(totalsMap)

        then:
        result == new BigDecimal('18.00')
        0 * _
    }

    void 'calculateGrandTotal - empty map returns zero'() {
        when:
        BigDecimal result = calculationService.calculateGrandTotal([:])

        then:
        result == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        0 * _
    }

    void 'createTotals - creates correct Totals object with grand total'() {
        given:
        BigDecimal future = new BigDecimal('2.00')
        BigDecimal cleared = new BigDecimal('5.00')
        BigDecimal outstanding = new BigDecimal('3.00')

        when:
        Totals result = calculationService.createTotals(future, cleared, outstanding)

        then:
        result.totalsFuture == future
        result.totalsCleared == cleared
        result.totalsOutstanding == outstanding
        result.totals == new BigDecimal('10.00')
        0 * _
    }

    void 'validateTotals - valid totals returns true'() {
        given:
        Totals totals = new Totals(
                new BigDecimal('2.00'),
                new BigDecimal('5.00'),
                new BigDecimal('10.00'),
                new BigDecimal('3.00')
        )

        when:
        Boolean result = calculationService.validateTotals(totals)

        then:
        result
        0 * _
    }

    void 'validateTotals - mismatched grand total returns false'() {
        given:
        Totals totals = new Totals(
                new BigDecimal('2.00'),
                new BigDecimal('5.00'),
                new BigDecimal('999.99'),
                new BigDecimal('3.00')
        )

        when:
        Boolean result = calculationService.validateTotals(totals)

        then:
        !result
        0 * _
    }

    void 'calculateActiveTotalsByAccountNameOwner - returns correct totals'() {
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
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

        then:
        result.totalsCleared == new BigDecimal('10.00')
        result.totalsOutstanding == new BigDecimal('5.00')
        result.totalsFuture == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        result.totals == new BigDecimal('15.00')
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> [cleared, outstanding]
        0 * _
    }

    void 'calculateActiveTotalsByAccountNameOwner - empty account returns zero totals'() {
        given:
        String accountNameOwner = 'empty_brian'

        when:
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

        then:
        result.totals == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        result.totalsCleared == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        result.totalsOutstanding == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> []
        0 * _
    }

    void 'calculateBonusProgress - below target not earned'() {
        given:
        String accountNameOwner = 'chase_brian'
        LocalDate startDate = LocalDate.of(2020, 1, 1)
        BigDecimal target = new BigDecimal('500.00')
        BigDecimal bonus = new BigDecimal('100.00')
        BigDecimal spent = new BigDecimal('200.00')
        BigDecimal spentPending = new BigDecimal('50.00')

        when:
        def result = calculationService.calculateBonusProgress(accountNameOwner, startDate, target, bonus, 90L)

        then:
        !result.bonusEarned
        result.spent == spent
        result.accountNameOwner == accountNameOwner
        1 * transactionRepositoryMock.sumSpendingInWindow(accountNameOwner, 'cleared', startDate, _) >> spent
        1 * transactionRepositoryMock.sumPendingSpendingInWindow(accountNameOwner, ['outstanding', 'future'], startDate, _) >> spentPending
        0 * _
    }

    void 'calculateBonusProgress - at or above target is earned'() {
        given:
        String accountNameOwner = 'chase_brian'
        LocalDate startDate = LocalDate.of(2020, 1, 1)
        BigDecimal target = new BigDecimal('500.00')
        BigDecimal bonus = new BigDecimal('100.00')
        BigDecimal spent = new BigDecimal('600.00')

        when:
        def result = calculationService.calculateBonusProgress(accountNameOwner, startDate, target, bonus, 90L)

        then:
        result.bonusEarned
        result.remaining == BigDecimal.ZERO
        1 * transactionRepositoryMock.sumSpendingInWindow(accountNameOwner, 'cleared', startDate, _) >> spent
        1 * transactionRepositoryMock.sumPendingSpendingInWindow(accountNameOwner, ['outstanding', 'future'], startDate, _) >> BigDecimal.ZERO
        0 * _
    }
}
