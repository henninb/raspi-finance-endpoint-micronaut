package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.repositories.AccountRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Singleton
open class AccountService(
    @Inject val accountRepository: AccountRepository,
    @Inject val transactionRepository: TransactionRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {
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
    open fun findAccountsThatRequirePayment(): List<String> {
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
    open fun insertAccount(account: Account): Boolean {
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

    // TODO: set the update timestamp logic
    @Timed
    open fun updateTheGrandTotalForAllClearedTransactions(): Boolean {
        //TODO: 1/6/2020 - add logic such that the logic is in the code and not the database
        //val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner()
//        accounts.forEach { account ->
//            //sum and update
//        }

        try {
            logger.info("updateAccountGrandTotals")
            accountRepository.updateTheGrandTotalForAllClearedTransactions()
            logger.info("updateAccountClearedTotals")
            accountRepository.updateTheGrandTotalForAllTransactions()
            logger.info("updateAccountTotals")

            //TODO: fix the exception
        } catch (invalidDataAccessResourceUsageException: Exception) {
            meterService.incrementExceptionCaughtCounter("InvalidDataAccessResourceUsageException")
            logger.warn("InvalidDataAccessResourceUsageException: ${invalidDataAccessResourceUsageException.message}")
        }
        return true
    }

    //TODO: Complete the method logic
    @Timed
    open fun updateAccount(account: Account): Boolean {
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
    open fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Boolean {
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

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}