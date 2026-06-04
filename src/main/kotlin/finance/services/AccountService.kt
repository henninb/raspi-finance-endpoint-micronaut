package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.repositories.AccountRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*
import finance.domain.TransactionState
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator

@Singleton
open class AccountService(
    @Inject val accountRepository: AccountRepository,
    @Inject val transactionRepository: TransactionRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) : IAccountService {

    @Timed
    override fun account(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    @Timed
    override fun accounts(): List<Account> {
        return accountRepository.findByActiveStatusOrderByAccountNameOwner()
    }

    // TODO: Implement these interface methods as needed
    override fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal = BigDecimal.ZERO
    override fun deleteAccount(accountNameOwner: String): Boolean = false
    @Timed
    open fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    @Timed
    open fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account> {
        val accounts = accountRepository.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts - found accounts.")
        }
        return accounts
    }

    @Timed
    open fun findByActiveStatusOrderByAccountNameOwner(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts - found accounts.")
        }
        return accounts
    }

    @Timed
    override fun findAccountsThatRequirePayment(): List<String> {
        return accountRepository.findAccountsThatRequirePayment()
    }

    @Timed
    open fun computeTheGrandTotalForAllTransactions(): BigDecimal {
        val totals: BigDecimal = accountRepository.computeTheGrandTotalForAllTransactions()
        return totals.setScale(2, RoundingMode.HALF_UP)
    }

    @Timed
    open fun computeTheGrandTotalForAllClearedTransactions(): BigDecimal {
        val totals: BigDecimal = accountRepository.computeTheGrandTotalForAllClearedTransactions()
        return totals.setScale(2, RoundingMode.HALF_UP)
    }

    @Timed
    override fun insertAccount(account: Account): Boolean {
        val accountOptional = findByAccountNameOwner(account.accountNameOwner)
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert account as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert account as there is a constraint violation on the data.")
        }

        if (!accountOptional.isPresent) {
            account.dateAdded = Timestamp(Calendar.getInstance().time.time)
            account.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            accountRepository.saveAndFlush(account)
            logger.info("inserted account successfully.")
        } else {
            logger.error("account not inserted as the account already exists ${account.accountNameOwner}.")
            return false
        }

        return true
    }

    @Timed
    open fun deleteByAccountNameOwner(accountNameOwner: String): Boolean {
        accountRepository.deleteByAccountNameOwner(accountNameOwner)
        return true
    }

    @Timed
    override fun updateTotalsForAllAccounts(): Boolean {
        try {
            logger.info("updateTotalsForAllAccounts")
            accountRepository.updateTotalsForAllAccounts()
        } catch (e: Exception) {
            meterService.incrementExceptionCaughtCounter("UpdateTotalsException")
            logger.warn("updateTotalsForAllAccounts failed: ${e.message}")
        }
        return true
    }

    // Keep legacy name as a delegate so callers in TransactionService/AccountController still compile
    @Timed
    open fun updateTheGrandTotalForAllClearedTransactions(): Boolean = updateTotalsForAllAccounts()

    //TODO: Complete the method logic
    @Timed
    override fun updateAccount(account: Account): Boolean {
        val optionalAccount = accountRepository.findByAccountNameOwner(account.accountNameOwner)
        if (optionalAccount.isPresent) {
            val accountToBeUpdated = optionalAccount.get()
            //account.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            logger.info("updated the account.")
            //var updateFlag = false
            //val fromDb = optionalAccount.get()
            accountRepository.saveAndFlush(accountToBeUpdated)
            return true
        }

        return false
    }

    @Timed
    override fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Boolean {
        val newAccountOptional = accountRepository.findByAccountNameOwner(newAccountNameOwner)
        val oldAccountOptional = accountRepository.findByAccountNameOwner(oldAccountNameOwner)

        if (!oldAccountOptional.isPresent) {
            throw RuntimeException("Cannot find the original account to rename: $oldAccountNameOwner")
        }
        if (newAccountOptional.isPresent) {
            throw RuntimeException("Cannot overwrite new account with an existing account : $newAccountNameOwner")
        }
        val oldAccount = oldAccountOptional.get()
        val newAccount = Account()
        newAccount.accountType = oldAccount.accountType
        newAccount.activeStatus = oldAccount.activeStatus
        newAccount.moniker = oldAccount.moniker
        newAccount.accountNameOwner = newAccountNameOwner
        val newlySavedAccount = accountRepository.saveAndFlush(newAccount)

        val transactions =
            transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(oldAccountNameOwner)
        transactions.forEach { transaction ->
            transaction.accountNameOwner = newlySavedAccount.accountNameOwner
            transaction.accountId = newlySavedAccount.accountId
            transaction.accountType = newlySavedAccount.accountType
            transaction.activeStatus = newlySavedAccount.activeStatus
            transactionRepository.saveAndFlush(transaction)
        }
        accountRepository.deleteByAccountNameOwner(oldAccountNameOwner)
        return true
    }

    @Timed
    open fun refreshValidationDates() {
        try {
            accountRepository.updateValidationDateForAllAccounts()
            logger.info("Refreshed validation dates for all active accounts")
        } catch (e: Exception) {
            logger.warn("Could not refresh validation dates: ${e.message}")
        }
    }

    @Timed
    open fun deactivateAccount(accountNameOwner: String): Account {
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (!accountOptional.isPresent) {
            throw RuntimeException("Account not found: $accountNameOwner")
        }
        val account = accountOptional.get()
        account.activeStatus = false
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        return accountRepository.saveAndFlush(account)
    }

    @Timed
    open fun activateAccount(accountNameOwner: String): Account {
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (!accountOptional.isPresent) {
            throw RuntimeException("Account not found: $accountNameOwner")
        }
        val account = accountOptional.get()
        account.activeStatus = true
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        return accountRepository.saveAndFlush(account)
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger(AccountService::class.java)
    }
}