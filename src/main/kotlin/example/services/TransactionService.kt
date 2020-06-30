package example.services

import example.domain.Transaction
import example.repositories.TransactionRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionService(@Inject val transactionRepository: TransactionRepository) {

    fun findByGuid(guid: String): Optional<Transaction> {
        //logger.info("call findByGuid")
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            return transactionOptional
        }
        return Optional.empty()
    }
}
