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

    override fun deleteAllPendingTransactions() : Boolean {
        pendingTransactionRepository.deleteAll()
        return true
    }
}