package finance.services

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.util.*

@Singleton
open class ValidationAmountService(
    private var validationAmountRepository: ValidationAmountRepository,
    private var accountRepository: AccountRepository
) : IValidationAmountService, BaseService() {




    override fun insertValidationAmount(
        accountNameOwner: String,
        validationAmount: ValidationAmount
    ): ValidationAmount {
        logger.info("Inserting validation amount for account: $accountNameOwner")
        var accountId = 0L
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            accountId = accountOptional.get().accountId
        } else {
            logger.warn("Account not found: $accountNameOwner")
        }

        val constraintViolations: Set<Any> = emptySet() // TODO: implement proper validation
        handleConstraintViolations(constraintViolations, meterService)

        validationAmount.accountId = accountId
        val timestamp = Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = timestamp
        validationAmount.dateUpdated = timestamp

        // Save the ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table
        if (accountOptional.isPresent) {
            val account = accountOptional.get()
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
            logger.info("Updated validation date for account: $accountNameOwner")
        }

        logger.info("Successfully inserted validation amount with ID: ${savedValidationAmount.validationId}")
        return savedValidationAmount
    }

    @Timed
    override fun findValidationAmountByAccountNameOwner(
        accountNameOwner: String,
        traansactionState: TransactionState
    ): ValidationAmount {
        logger.info("Finding validation amount for account: $accountNameOwner, state: $traansactionState")
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            val validationAmountList = validationAmountRepository.findByTransactionStateAndAccountId(
                traansactionState,
                accountOptional.get().accountId
            )
            if (validationAmountList.isEmpty()) {
                logger.info("No validation amounts found for account: $accountNameOwner")
                return ValidationAmount()
            }
            val latestValidation = validationAmountList.sortedByDescending { it.validationDate }.first()
            logger.info("Found validation amount for account: $accountNameOwner")
            return latestValidation
        }
        logger.warn("Account not found: $accountNameOwner")
        return ValidationAmount()
    }

    open fun findAllActive(): List<ValidationAmount> =
        validationAmountRepository.findByActiveStatusTrue()

    open fun findById(validationId: Long): Optional<ValidationAmount> =
        validationAmountRepository.findById(validationId)

    open fun updateValidationAmount(validationId: Long, validationAmount: ValidationAmount): Optional<ValidationAmount> {
        val existing = validationAmountRepository.findById(validationId)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.amount = validationAmount.amount
            toUpdate.transactionState = validationAmount.transactionState
            toUpdate.validationDate = validationAmount.validationDate
            toUpdate.activeStatus = validationAmount.activeStatus
            toUpdate.dateUpdated = Timestamp(System.currentTimeMillis())
            return Optional.of(validationAmountRepository.saveAndFlush(toUpdate))
        }
        return Optional.empty()
    }

    open fun deleteById(validationId: Long): Boolean {
        val existing = validationAmountRepository.findById(validationId)
        if (existing.isPresent) {
            validationAmountRepository.delete(existing.get())
            return true
        }
        return false
    }
}