package finance.services

import finance.domain.BonusProgress
import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.TransactionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Singleton
open class CalculationService(
    @Inject private val transactionRepository: TransactionRepository,
    @Inject private val meterService: MeterService,
) : ICalculationService {

    companion object {
        private val logger = LogManager.getLogger(CalculationService::class.java)
        private const val MAX_REASONABLE_AMOUNT = 999_999_999.99
        private val ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    override fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals {
        return try {
            val transactions = transactionRepository
                .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner)
            val totalsMap = calculateTotalsFromTransactions(transactions)
            createTotals(
                totalsFuture = totalsMap[TransactionState.Future] ?: ZERO,
                totalsCleared = totalsMap[TransactionState.Cleared] ?: ZERO,
                totalsOutstanding = totalsMap[TransactionState.Outstanding] ?: ZERO,
            )
        } catch (ex: Exception) {
            logger.error("Error calculating totals for account $accountNameOwner: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsCalculationError")
            throw ex
        }
    }

    override fun calculateTotalsFromTransactions(transactions: List<Transaction>): Map<TransactionState, BigDecimal> {
        return try {
            val totalsMap = mutableMapOf<TransactionState, BigDecimal>()
            transactions.forEach { transaction ->
                val state = transaction.transactionState
                val current = totalsMap[state] ?: BigDecimal.ZERO
                totalsMap[state] = current.add(transaction.amount).setScale(2, RoundingMode.HALF_UP)
            }
            totalsMap.toMap()
        } catch (ex: Exception) {
            logger.error("Error calculating totals from transactions: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("InMemoryTotalsCalculationError")
            emptyMap()
        }
    }

    override fun calculateGrandTotal(totalsMap: Map<TransactionState, BigDecimal>): BigDecimal {
        return try {
            totalsMap.values
                .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                .setScale(2, RoundingMode.HALF_UP)
        } catch (ex: Exception) {
            logger.error("Error calculating grand total: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("GrandTotalCalculationError")
            ZERO
        }
    }

    override fun createTotals(
        totalsFuture: BigDecimal,
        totalsCleared: BigDecimal,
        totalsOutstanding: BigDecimal,
    ): Totals {
        return try {
            val grandTotal = calculateGrandTotal(
                mapOf(
                    TransactionState.Future to totalsFuture,
                    TransactionState.Cleared to totalsCleared,
                    TransactionState.Outstanding to totalsOutstanding,
                )
            )
            Totals(
                totalsFuture = totalsFuture,
                totalsCleared = totalsCleared,
                totals = grandTotal,
                totalsOutstanding = totalsOutstanding,
            )
        } catch (ex: Exception) {
            logger.error("Error creating Totals object: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsCreationError")
            Totals(ZERO, ZERO, ZERO, ZERO)
        }
    }

    open fun calculateBonusProgress(
        accountNameOwner: String,
        startDate: LocalDate,
        targetAmount: BigDecimal,
        bonusAmount: BigDecimal,
        windowDays: Long = 90L,
    ): BonusProgress {
        val windowEndDate = startDate.plusDays(windowDays - 1)
        val today = LocalDate.now()
        val start = Date.valueOf(startDate)
        val end = Date.valueOf(windowEndDate)

        val spent = transactionRepository.sumSpendingInWindow(
            accountNameOwner, "cleared", start, end
        )
        val spentPending = transactionRepository.sumPendingSpendingInWindow(
            accountNameOwner, listOf("outstanding", "future"), start, end
        )
        val remaining = (targetAmount - spent).max(BigDecimal.ZERO)
        val percentComplete = if (targetAmount > BigDecimal.ZERO)
            (spent.toDouble() / targetAmount.toDouble() * 100.0).coerceAtMost(100.0)
        else 0.0
        val daysRemaining = if (today.isAfter(windowEndDate)) 0L else ChronoUnit.DAYS.between(today, windowEndDate)

        return BonusProgress(
            accountNameOwner = accountNameOwner,
            spent = spent,
            spentPending = spentPending,
            target = targetAmount,
            remaining = remaining,
            percentComplete = Math.round(percentComplete * 10.0) / 10.0,
            bonusAmount = bonusAmount,
            bonusEarned = spent >= targetAmount,
            windowStartDate = startDate,
            windowEndDate = windowEndDate,
            daysRemaining = daysRemaining,
        )
    }

    override fun validateTotals(totals: Totals): Boolean {
        return try {
            val amounts = listOf(totals.totalsFuture, totals.totalsCleared, totals.totalsOutstanding, totals.totals)
            val allReasonable = amounts.all { it.abs().compareTo(BigDecimal(MAX_REASONABLE_AMOUNT)) <= 0 }
            if (!allReasonable) {
                logger.warn("Totals validation failed: amounts exceed reasonable limits")
                return false
            }
            val expectedGrandTotal = calculateGrandTotal(
                mapOf(
                    TransactionState.Future to totals.totalsFuture,
                    TransactionState.Cleared to totals.totalsCleared,
                    TransactionState.Outstanding to totals.totalsOutstanding,
                )
            )
            val matches = totals.totals.compareTo(expectedGrandTotal) == 0
            if (!matches) {
                logger.warn("Totals validation failed: grand total ${totals.totals} != expected $expectedGrandTotal")
            }
            matches
        } catch (ex: Exception) {
            logger.error("Error validating totals: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsValidationError")
            false
        }
    }
}
