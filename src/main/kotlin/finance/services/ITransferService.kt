package finance.services

import finance.domain.Transfer
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

interface ITransferService {
    fun findAllTransfers(): List<Transfer>
    fun findAllTransfersPaged(pageable: Pageable): Page<Transfer>

    fun insertTransfer(transfer: Transfer): Transfer
//
//    fun populateDebitTransaction(
//        transactionDebit: Transaction,
//        transfer: Transfer,
//        transferAccountNameOwner: String
//    )
//
//    fun populateCreditTransaction(
//        transactionCredit: Transaction,
//        transfer: Transfer,
//        transferAccountNameOwner: String
//    )

    fun updateTransfer(transferId: Long, transfer: Transfer): Optional<Transfer>
    fun deleteByTransferId(transferId: Long): Boolean
    fun findByTransferId(transferId: Long): Optional<Transfer>
}