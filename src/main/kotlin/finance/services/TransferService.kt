package finance.services

import finance.domain.*
import finance.exceptions.DuplicateTransferException
import finance.repositories.TransferRepository
import io.micrometer.core.annotation.Timed
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Singleton
open class TransferService(
    private var transferRepository: TransferRepository,
    private var transactionService: TransactionService,
    private var accountService: AccountService
) : ITransferService, BaseService() {

    @Timed
    override fun findAllTransfers(): List<Transfer> {
        logger.info("Fetching all transfers")
        val transfers = transferRepository.findAll().sortedByDescending { transfer -> transfer.transactionDate }
        logger.info("Found ${transfers.size} transfers")
        return transfers
    }

    @Timed
    override fun findAllTransfersPaged(pageable: Pageable): Page<Transfer> {
        val sort = Sort.of(Sort.Order.desc("transactionDate"))
        val sortedPageable = Pageable.from(pageable.number, pageable.size, sort)
        return transferRepository.findByActiveStatusOrderByTransactionDateDesc(true, sortedPageable)
    }


    @Timed
    @Transactional
    override fun insertTransfer(transfer: Transfer): Transfer {
        // Validate source account
        val optionalSourceAccount = accountService.account(transfer.sourceAccount)
        if (!optionalSourceAccount.isPresent) {
            logger.error("Source account not found: ${transfer.sourceAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Source account not found: ${transfer.sourceAccount}")
        }
        transfer.owner = optionalSourceAccount.get().owner ?: ""

        // Validate destination account
        val optionalDestinationAccount = accountService.account(transfer.destinationAccount)
        if (!optionalDestinationAccount.isPresent) {
            logger.error("Destination account not found: ${transfer.destinationAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Destination account not found: ${transfer.destinationAccount}")
        }

        val transactionSource = buildTransferTransaction(
            transfer = transfer,
            accountName = transfer.sourceAccount,
            description = "transfer withdrawal",
            notes = "Transfer to ${transfer.destinationAccount}",
            amount = transfer.amount.negate(),
            accountType = AccountType.Debit
        )
        val transactionDestination = buildTransferTransaction(
            transfer = transfer,
            accountName = transfer.destinationAccount,
            description = "transfer deposit",
            notes = "Transfer from ${transfer.sourceAccount}",
            amount = transfer.amount,
            accountType = AccountType.Credit
        )

        try {
            transactionService.insertTransaction(transactionSource)
            transactionService.insertTransaction(transactionDestination)

            transfer.guidSource = transactionSource.guid
            transfer.guidDestination = transactionDestination.guid
            logger.info("Creating transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")
            val timestamp = Timestamp(System.currentTimeMillis())
            transfer.dateUpdated = timestamp
            transfer.dateAdded = timestamp

            val savedTransfer = transferRepository.saveAndFlush(transfer)
            logger.info("Successfully created transfer with ID: ${savedTransfer.transferId}")
            return savedTransfer
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("duplicate", ignoreCase = true) || msg.contains("unique", ignoreCase = true)) {
                logger.error("Duplicate transfer detected: ${transfer.sourceAccount} -> ${transfer.destinationAccount} on ${transfer.transactionDate}")
                meterService.incrementExceptionThrownCounter("DuplicateTransferException")
                throw DuplicateTransferException("Transfer already exists for ${transfer.sourceAccount} -> ${transfer.destinationAccount} on ${transfer.transactionDate}")
            }
            throw e
        }
    }

    private fun buildTransferTransaction(
        transfer: Transfer,
        accountName: String,
        description: String,
        notes: String,
        amount: BigDecimal,
        accountType: AccountType
    ): Transaction {
        val timestamp = Timestamp(System.currentTimeMillis())
        return Transaction().apply {
            guid = UUID.randomUUID().toString()
            transactionDate = transfer.transactionDate
            this.description = description
            category = "transfer"
            this.notes = notes
            this.amount = amount
            transactionState = TransactionState.Outstanding
            reoccurringType = ReoccurringType.Onetime
            transactionType = "transfer"
            this.accountType = accountType
            accountNameOwner = accountName
            owner = transfer.owner ?: ""
            dateUpdated = timestamp
            dateAdded = timestamp
        }
    }

    @Timed
    override fun deleteByTransferId(transferId: Long): Boolean {
        logger.info("Deleting transfer with ID: $transferId")
        val transferOptional = transferRepository.findByTransferId(transferId)
        if (transferOptional.isPresent) {
            val transfer = transferOptional.get()
            val guidSource = transfer.guidSource
            val guidDestination = transfer.guidDestination
            // Delete the transfer record first to remove FK references to t_transaction
            transferRepository.delete(transfer)
            // Then delete the linked transactions
            if (!guidSource.isNullOrBlank()) {
                transactionService.deleteTransactionByGuid(guidSource)
                logger.info("Deleted source transaction: $guidSource")
            }
            if (!guidDestination.isNullOrBlank()) {
                transactionService.deleteTransactionByGuid(guidDestination)
                logger.info("Deleted destination transaction: $guidDestination")
            }
            logger.info("Successfully deleted transfer with ID: $transferId")
            return true
        }
        logger.warn("Transfer not found with ID: $transferId")
        return false
    }

    @Timed
    override fun updateTransfer(transferId: Long, transfer: Transfer): Optional<Transfer> {
        val existing = transferRepository.findByTransferId(transferId)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.sourceAccount = transfer.sourceAccount
            toUpdate.destinationAccount = transfer.destinationAccount
            toUpdate.transactionDate = transfer.transactionDate
            toUpdate.amount = transfer.amount
            toUpdate.activeStatus = transfer.activeStatus
            return Optional.of(transferRepository.saveAndFlush(toUpdate))
        }
        return Optional.empty()
    }

    @Timed
    override fun findByTransferId(transferId: Long): Optional<Transfer> {
        logger.info("service - findByTransferId = $transferId")
        val transferOptional: Optional<Transfer> = transferRepository.findByTransferId(transferId)
        if (transferOptional.isPresent) {
            return transferOptional
        }
        return Optional.empty()
    }
}