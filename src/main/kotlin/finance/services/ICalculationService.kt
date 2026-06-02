package finance.services

import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import java.math.BigDecimal

interface ICalculationService {
    fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals
    fun calculateTotalsFromTransactions(transactions: List<Transaction>): Map<TransactionState, BigDecimal>
    fun calculateGrandTotal(totalsMap: Map<TransactionState, BigDecimal>): BigDecimal
    fun createTotals(totalsFuture: BigDecimal, totalsCleared: BigDecimal, totalsOutstanding: BigDecimal): Totals
    fun validateTotals(totals: Totals): Boolean
}
