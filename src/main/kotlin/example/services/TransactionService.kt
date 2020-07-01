package example.services

import example.domain.Account
import example.domain.AccountType
import example.domain.Category
import example.domain.Transaction
import example.repositories.TransactionRepository
import java.sql.Timestamp
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Singleton
class TransactionService(@Inject val transactionRepository: TransactionRepository,
                         @Inject val validator: Validator,
                         @Inject val categoryService: CategoryService,
                         @Inject val accountService: AccountService) {

    fun findByGuid(guid: String): Optional<Transaction> {
        //logger.info("call findByGuid")
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            return transactionOptional
        }
        return Optional.empty()
    }

    fun insertTransaction(transaction: Transaction): Boolean {

        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        if (constraintViolations.isNotEmpty()) {
            //logger.info("insertTransaction() ConstraintViolation")
        }
        //logger.info("*** insert transaction ***")
        val transactionOptional = findByGuid(transaction.guid)

        if (transactionOptional.isPresent) {
            val transactionDb = transactionOptional.get()
            //logger.info("*** update transaction ***")
            return updateTransaction(transactionDb, transaction)
        }

        processAccount(transaction)
        processCategory(transaction)
        println("transaction = ${transaction}")
        transactionRepository.save(transaction)
        //logger.info("*** inserted transaction ***")
        return true
    }

    private fun processAccount(transaction: Transaction) {
        var accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
        if (accountOptional.isPresent) {
            //logger.info("METRIC_ACCOUNT_ALREADY_EXISTS_COUNTER")
            //transaction.accountId = accountOptional.get().accountId
        } else {
            //logger.info("METRIC_ACCOUNT_NOT_FOUND_COUNTER")
            val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
            //logger.debug("will insertAccount")
            accountService.insertAccount(account)
            //logger.debug("called insertAccount")
            accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
            //transaction.accountId = accountOptional.get().accountId
            //meterRegistry.counter(METRIC_ACCOUNT_NOT_FOUND_COUNTER).increment()
        }
    }

    private fun processCategory(transaction: Transaction) {
//        when {
//            transaction.category != "" -> {
//                val optionalCategory = categoryService.findByCategory(transaction.category)
//                if (optionalCategory.isPresent) {
//                    transaction.categories.add(optionalCategory.get())
//                } else {
//                    val category = createDefaultCategory(transaction.category)
//                    categoryService.insertCategory(category)
//                    transaction.categories.add(category)
//                }
//            }
//        }
    }

    private fun updateTransaction(transactionDb: Transaction, transaction: Transaction): Boolean {
//        if (transactionDb.accountNameOwner.trim() == transaction.accountNameOwner) {
//
//            if (transactionDb.amount != transaction.amount) {
//                //logger.info("discrepancy in the amount for <${transactionDb.guid}>")
//                //TODO: metric for this
//                transactionRepository.setAmountByGuid(transaction.amount, transaction.guid)
//                return true
//            }
//
//            if (transactionDb.cleared != transaction.cleared) {
//                //logger.info("discrepancy in the cleared value for <${transactionDb.guid}>")
//                //TODO: metric for this
//                transactionRepository.setClearedByGuid(transaction.cleared, transaction.guid)
//                return true
//            }
//            return true
//        }

        //logger.info("transaction already exists, no transaction data inserted.")
        return false
    }

    private fun createDefaultCategory(categoryName: String): Category {
        val category = Category()

        category.category = categoryName
        return category
    }

    private fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()

        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        account.dateAdded = Timestamp(System.currentTimeMillis())
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        return account
    }

}
