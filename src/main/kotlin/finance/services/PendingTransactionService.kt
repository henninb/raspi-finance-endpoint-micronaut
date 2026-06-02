package finance.services

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.util.*


@Singleton
open class PendingTransactionService(
        private var pendingTransactionRepository: PendingTransactionRepository,
    ) : IPendingTransactionService, BaseService() {


    override fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
        pendingTransaction.dateAdded = Timestamp(Calendar.getInstance().time.time)
        return pendingTransactionRepository.saveAndFlush(pendingTransaction)
    }

    override fun deletePendingTransaction(pendingTransactionId: Long): Boolean {
        val category = pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId).get()
        pendingTransactionRepository.delete(category)
        return true
    }

    override fun getAllPendingTransactions(): List<PendingTransaction> {
        return pendingTransactionRepository.findAll()
    }

    override fun deleteAllPendingTransactions(): Boolean {
        pendingTransactionRepository.deleteAll()
        return true
    }

    open fun findByPendingTransactionId(id: Long): Optional<PendingTransaction> =
        pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(id)

    open fun updatePendingTransaction(id: Long, pendingTransaction: PendingTransaction): Optional<PendingTransaction> {
        val existing = pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(id)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.accountNameOwner = pendingTransaction.accountNameOwner
            toUpdate.description = pendingTransaction.description
            toUpdate.amount = pendingTransaction.amount
            toUpdate.transactionDate = pendingTransaction.transactionDate
            return Optional.of(pendingTransactionRepository.saveAndFlush(toUpdate))
        }
        return Optional.empty()
    }
}