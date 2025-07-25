package finance.services

import finance.domain.*
import finance.repositories.TransferRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Singleton
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
    override fun insertTransfer(transfer: Transfer): Transfer {
        val transactionSource = Transaction()
        val transactionDestination = Transaction()

        // Validate transfer - simplified validation
        val constraintViolations: Set<Any> = emptySet() // TODO: implement proper validation
        handleConstraintViolations(constraintViolations, meterService)

        // Validate source account
        val optionalSourceAccount = accountService.account(transfer.sourceAccount)
        if (!optionalSourceAccount.isPresent) {
            logger.error("Source account not found: ${transfer.sourceAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Source account not found: ${transfer.sourceAccount}")
        }

        // Validate destination account
        val optionalDestinationAccount = accountService.account(transfer.destinationAccount)
        if (!optionalDestinationAccount.isPresent) {
            logger.error("Destination account not found: ${transfer.destinationAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Destination account not found: ${transfer.destinationAccount}")
        }

        // Populate source and destination transactions
        populateSourceTransaction(transactionSource, transfer, transfer.sourceAccount)
        populateDestinationTransaction(transactionDestination, transfer, transfer.destinationAccount)

        // Save transactions and transfer
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
    }

    private fun populateSourceTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer withdrawal"
        transaction.category = "transfer"
        transaction.notes = "Transfer to ${transfer.destinationAccount}"
        transaction.amount = transfer.amount.negate()
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Debit
        transaction.accountNameOwner = accountName
        val timestamp = Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = timestamp
        transaction.dateAdded = timestamp
    }

    private fun populateDestinationTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer deposit"
        transaction.category = "transfer"
        transaction.notes = "Transfer from ${transfer.sourceAccount}"
        transaction.amount = transfer.amount
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Debit
        transaction.accountNameOwner = accountName
        val timestamp = Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = timestamp
        transaction.dateAdded = timestamp
    }

    @Timed
    override fun deleteByTransferId(transferId: Long): Boolean {
        logger.info("Deleting transfer with ID: $transferId")
        val transferOptional = transferRepository.findByTransferId(transferId)
        if (transferOptional.isPresent) {
            transferRepository.delete(transferOptional.get())
            logger.info("Successfully deleted transfer with ID: $transferId")
            return true
        }
        logger.warn("Transfer not found with ID: $transferId")
        return false
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