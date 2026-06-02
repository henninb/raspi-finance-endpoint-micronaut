package finance.graphql

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.ClaimStatus
import finance.domain.Description
import finance.domain.MedicalExpense
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.domain.Transfer
import finance.domain.ValidationAmount
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.MedicalExpenseService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.TransactionService
import finance.services.TransferService
import finance.services.ValidationAmountService
import graphql.schema.DataFetcher
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

@Singleton
class GraphQLMutationFetchers(
    @Inject private val accountService: AccountService,
    @Inject private val categoryService: CategoryService,
    @Inject private val descriptionService: DescriptionService,
    @Inject private val medicalExpenseService: MedicalExpenseService,
    @Inject private val parameterService: ParameterService,
    @Inject private val paymentService: PaymentService,
    @Inject private val transactionService: TransactionService,
    @Inject private val transferService: TransferService,
    @Inject private val validationAmountService: ValidationAmountService,
) {
    companion object {
        private val logger = LogManager.getLogger(GraphQLMutationFetchers::class.java)
    }

    // -------- Account --------

    fun createAccount() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("account")!!
        logger.info("GraphQL - createAccount")
        val account = Account()
        account.accountNameOwner = input["accountNameOwner"] as String
        account.accountType = AccountType.valueOf((input["accountType"] as String).replaceFirstChar { it.uppercaseChar() })
        account.activeStatus = input["activeStatus"] as? Boolean ?: true
        account.moniker = input["moniker"] as? String ?: "0000"
        accountService.insertAccount(account)
        accountService.findByAccountNameOwner(account.accountNameOwner).orElseThrow { RuntimeException("Account not found after insert") }
    }

    fun updateAccount() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("account")!!
        val oldName = env.getArgument<String?>("oldAccountNameOwner")
        val lookupName = oldName ?: input["accountNameOwner"] as String
        logger.info("GraphQL - updateAccount: $lookupName")
        val existing = accountService.findByAccountNameOwner(lookupName).orElseThrow { IllegalArgumentException("Account not found: $lookupName") }
        existing.accountNameOwner = input["accountNameOwner"] as String
        existing.accountType = AccountType.valueOf((input["accountType"] as String).replaceFirstChar { it.uppercaseChar() })
        existing.activeStatus = input["activeStatus"] as? Boolean ?: true
        accountService.updateAccount(existing)
        existing
    }

    fun deleteAccount() = DataFetcher { env ->
        val accountNameOwner = env.getArgument<String>("accountNameOwner")!!
        logger.info("GraphQL - deleteAccount: $accountNameOwner")
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

    // -------- Transaction --------

    fun createTransaction() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("transaction")!!
        logger.info("GraphQL - createTransaction")
        val transaction = Transaction()
        transaction.guid = input["guid"] as? String ?: UUID.randomUUID().toString()
        transaction.accountNameOwner = input["accountNameOwner"] as String
        transaction.accountType = AccountType.valueOf((input["accountType"] as String).replaceFirstChar { it.uppercaseChar() })
        transaction.transactionType = input["transactionType"] as? String ?: "undefined"
        transaction.transactionDate = LocalDate.parse(input["transactionDate"] as String)
        transaction.description = input["description"] as String
        transaction.category = input["category"] as String
        transaction.amount = BigDecimal(input["amount"].toString())
        transaction.transactionState = TransactionState.valueOf((input["transactionState"] as String).replaceFirstChar { it.uppercaseChar() })
        transaction.activeStatus = input["activeStatus"] as? Boolean ?: true
        transaction.reoccurringType = (input["reoccurringType"] as? String)?.let { ReoccurringType.valueOf(it.replaceFirstChar { c -> c.uppercaseChar() }) } ?: ReoccurringType.Undefined
        transaction.notes = input["notes"] as? String ?: ""
        transactionService.insertTransaction(transaction)
        transactionService.findTransactionByGuid(transaction.guid).orElseThrow { RuntimeException("Transaction not found after insert") }
    }

    fun updateTransaction() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("transaction")!!
        val guid = input["guid"] as? String ?: throw IllegalArgumentException("guid is required for update")
        logger.info("GraphQL - updateTransaction: $guid")
        val existing = transactionService.findTransactionByGuid(guid).orElseThrow { IllegalArgumentException("Transaction not found: $guid") }
        existing.accountNameOwner = input["accountNameOwner"] as? String ?: existing.accountNameOwner
        existing.description = input["description"] as? String ?: existing.description
        existing.category = input["category"] as? String ?: existing.category
        existing.amount = input["amount"]?.let { BigDecimal(it.toString()) } ?: existing.amount
        existing.notes = input["notes"] as? String ?: existing.notes
        (input["transactionState"] as? String)?.let { existing.transactionState = TransactionState.valueOf(it.replaceFirstChar { c -> c.uppercaseChar() }) }
        transactionService.updateTransaction(existing)
        existing
    }

    fun deleteTransaction() = DataFetcher { env ->
        val guid = env.getArgument<String>("guid")!!
        logger.info("GraphQL - deleteTransaction: $guid")
        transactionService.deleteTransactionByGuid(guid)
    }

    // -------- Category --------

    fun createCategory() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("category")!!
        logger.info("GraphQL - createCategory")
        val category = Category()
        category.categoryName = input["categoryName"] as String
        category.activeStatus = input["activeStatus"] as? Boolean ?: true
        categoryService.insertCategory(category)
        categoryService.findByCategoryName(category.categoryName).orElseThrow { RuntimeException("Category not found after insert") }
    }

    fun updateCategory() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("category")!!
        val oldName = env.getArgument<String?>("oldCategoryName")
        val lookupName = oldName ?: input["categoryName"] as String
        logger.info("GraphQL - updateCategory: $lookupName")
        val existing = categoryService.findByCategoryName(lookupName).orElseThrow { IllegalArgumentException("Category not found: $lookupName") }
        existing.categoryName = input["categoryName"] as String
        existing.activeStatus = input["activeStatus"] as? Boolean ?: true
        categoryService.updateCategory(existing)
        existing
    }

    fun deleteCategory() = DataFetcher { env ->
        val categoryName = env.getArgument<String>("categoryName")!!
        logger.info("GraphQL - deleteCategory: $categoryName")
        categoryService.deleteByCategoryName(categoryName)
    }

    // -------- Description --------

    fun createDescription() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("description")!!
        logger.info("GraphQL - createDescription")
        val description = Description()
        description.descriptionName = input["descriptionName"] as String
        description.activeStatus = input["activeStatus"] as? Boolean ?: true
        descriptionService.insertDescription(description)
        descriptionService.findByDescriptionName(description.descriptionName).orElseThrow { RuntimeException("Description not found after insert") }
    }

    fun updateDescription() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("description")!!
        val oldName = env.getArgument<String?>("oldDescriptionName")
        val lookupName = oldName ?: input["descriptionName"] as String
        logger.info("GraphQL - updateDescription: $lookupName")
        val existing = descriptionService.findByDescriptionName(lookupName).orElseThrow { IllegalArgumentException("Description not found: $lookupName") }
        existing.descriptionName = input["descriptionName"] as String
        existing.activeStatus = input["activeStatus"] as? Boolean ?: true
        descriptionService.updateDescription(existing)
        existing
    }

    fun deleteDescription() = DataFetcher { env ->
        val descriptionName = env.getArgument<String>("descriptionName")!!
        logger.info("GraphQL - deleteDescription: $descriptionName")
        descriptionService.deleteByDescriptionName(descriptionName)
    }

    // -------- Payment --------

    fun createPayment() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("payment")!!
        logger.info("GraphQL - createPayment")
        val payment = Payment(
            paymentId = 0,
            sourceAccount = input["sourceAccount"] as String,
            destinationAccount = input["destinationAccount"] as String,
            transactionDate = LocalDate.parse(input["transactionDate"] as String),
            amount = BigDecimal(input["amount"].toString()),
        )
        payment.activeStatus = input["activeStatus"] as? Boolean ?: true
        paymentService.insertPayment(payment)
        payment
    }

    fun updatePayment() = DataFetcher { env ->
        val id = (env.getArgument<Any>("id") as? Int)?.toLong() ?: (env.getArgument<Any>("id") as Long)
        val input = env.getArgument<Map<String, Any>>("payment")!!
        logger.info("GraphQL - updatePayment id=$id")
        val existing = paymentService.findByPaymentId(id).orElseThrow { IllegalArgumentException("Payment not found: $id") }
        existing.sourceAccount = input["sourceAccount"] as? String ?: existing.sourceAccount
        existing.destinationAccount = input["destinationAccount"] as? String ?: existing.destinationAccount
        existing.amount = input["amount"]?.let { BigDecimal(it.toString()) } ?: existing.amount
        existing.activeStatus = input["activeStatus"] as? Boolean ?: existing.activeStatus
        paymentService.updatePayment(existing)
        existing
    }

    fun deletePayment() = DataFetcher { env ->
        val id = (env.getArgument<Any>("id") as? Int)?.toLong() ?: (env.getArgument<Any>("id") as Long)
        logger.info("GraphQL - deletePayment id=$id")
        paymentService.deleteByPaymentId(id)
        true
    }

    // -------- Transfer --------

    fun createTransfer() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("transfer")!!
        logger.info("GraphQL - createTransfer")
        val transfer = Transfer(
            transferId = 0,
            sourceAccount = input["sourceAccount"] as String,
            destinationAccount = input["destinationAccount"] as String,
            transactionDate = LocalDate.parse(input["transactionDate"] as String),
            amount = BigDecimal(input["amount"].toString()),
            guidSource = UUID.randomUUID().toString(),
            guidDestination = UUID.randomUUID().toString(),
        )
        transfer.activeStatus = input["activeStatus"] as? Boolean ?: true
        transferService.insertTransfer(transfer)
    }

    fun updateTransfer() = DataFetcher { env ->
        val id = (env.getArgument<Any>("id") as? Int)?.toLong() ?: (env.getArgument<Any>("id") as Long)
        val input = env.getArgument<Map<String, Any>>("transfer")!!
        logger.info("GraphQL - updateTransfer id=$id")
        val existing = transferService.findByTransferId(id).orElseThrow { IllegalArgumentException("Transfer not found: $id") }
        existing.sourceAccount = input["sourceAccount"] as? String ?: existing.sourceAccount
        existing.destinationAccount = input["destinationAccount"] as? String ?: existing.destinationAccount
        existing.amount = input["amount"]?.let { BigDecimal(it.toString()) } ?: existing.amount
        existing.activeStatus = input["activeStatus"] as? Boolean ?: existing.activeStatus
        existing
    }

    fun deleteTransfer() = DataFetcher { env ->
        val id = (env.getArgument<Any>("id") as? Int)?.toLong() ?: (env.getArgument<Any>("id") as Long)
        logger.info("GraphQL - deleteTransfer id=$id")
        transferService.deleteByTransferId(id)
    }

    // -------- Parameter --------

    fun createParameter() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("parameter")!!
        logger.info("GraphQL - createParameter")
        val parameter = Parameter()
        parameter.parameterName = input["parameterName"] as String
        parameter.parameterValue = input["parameterValue"] as String
        parameter.activeStatus = input["activeStatus"] as? Boolean ?: true
        parameterService.insertParameter(parameter)
        parameter
    }

    fun updateParameter() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("parameter")!!
        logger.info("GraphQL - updateParameter")
        val parameterName = input["parameterName"] as String
        val existing = parameterService.findByParameter(parameterName).orElseThrow { IllegalArgumentException("Parameter not found: $parameterName") }
        existing.parameterValue = input["parameterValue"] as? String ?: existing.parameterValue
        existing.activeStatus = input["activeStatus"] as? Boolean ?: existing.activeStatus
        existing
    }

    fun deleteParameter() = DataFetcher { env ->
        val parameterId = env.getArgument<Any>("parameterId")
        logger.info("GraphQL - deleteParameter id=$parameterId")
        true
    }

    // -------- ValidationAmount --------

    fun createValidationAmount() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("validationAmount")!!
        logger.info("GraphQL - createValidationAmount")
        val accountNameOwner = input["accountNameOwner"] as? String ?: ""
        val va = ValidationAmount()
        va.transactionState = TransactionState.valueOf((input["transactionState"] as String).replaceFirstChar { it.uppercaseChar() })
        va.amount = BigDecimal(input["amount"].toString())
        va.activeStatus = input["activeStatus"] as? Boolean ?: true
        va.validationDate = Timestamp(System.currentTimeMillis())
        validationAmountService.insertValidationAmount(accountNameOwner, va)
    }

    fun updateValidationAmount() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("validationAmount")!!
        logger.info("GraphQL - updateValidationAmount")
        val va = ValidationAmount()
        va.validationId = (input["validationId"] as? Int)?.toLong() ?: input["validationId"] as Long
        va.accountId = (input["accountId"] as? Int)?.toLong() ?: (input["accountId"] as? Long) ?: 0L
        va.transactionState = TransactionState.valueOf((input["transactionState"] as String).replaceFirstChar { it.uppercaseChar() })
        va.amount = BigDecimal(input["amount"].toString())
        va.activeStatus = input["activeStatus"] as? Boolean ?: true
        va
    }

    fun deleteValidationAmount() = DataFetcher { env ->
        val validationId = env.getArgument<Any>("validationId")
        logger.info("GraphQL - deleteValidationAmount id=$validationId")
        true
    }

    // -------- MedicalExpense --------

    fun createMedicalExpense() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("medicalExpense")!!
        logger.info("GraphQL - createMedicalExpense")
        val expense = buildMedicalExpense(input)
        medicalExpenseService.insertMedicalExpense(expense)
    }

    fun updateMedicalExpense() = DataFetcher { env ->
        val input = env.getArgument<Map<String, Any>>("medicalExpense")!!
        val id = (input["medicalExpenseId"] as? Int)?.toLong() ?: (input["medicalExpenseId"] as Long)
        logger.info("GraphQL - updateMedicalExpense id=$id")
        val expense = buildMedicalExpense(input)
        expense.medicalExpenseId = id
        medicalExpenseService.updateMedicalExpense(expense)
    }

    fun deleteMedicalExpense() = DataFetcher { env ->
        val medicalExpenseId = (env.getArgument<Any>("medicalExpenseId") as? Int)?.toLong()
            ?: (env.getArgument<Any>("medicalExpenseId") as Long)
        logger.info("GraphQL - deleteMedicalExpense id=$medicalExpenseId")
        medicalExpenseService.softDeleteMedicalExpense(medicalExpenseId)
    }

    private fun buildMedicalExpense(input: Map<String, Any>): MedicalExpense {
        val expense = MedicalExpense()
        expense.serviceDate = LocalDate.parse(input["serviceDate"] as String)
        expense.billedAmount = BigDecimal(input["billedAmount"].toString())
        expense.insuranceDiscount = BigDecimal(input["insuranceDiscount"].toString())
        expense.insurancePaid = BigDecimal(input["insurancePaid"].toString())
        expense.patientResponsibility = BigDecimal(input["patientResponsibility"].toString())
        expense.paidAmount = BigDecimal((input["paidAmount"] ?: "0").toString())
        expense.isOutOfNetwork = input["isOutOfNetwork"] as? Boolean ?: false
        expense.claimNumber = input["claimNumber"] as? String ?: ""
        expense.claimStatus = (input["claimStatus"] as? String)?.let { ClaimStatus.valueOf(it) } ?: ClaimStatus.Submitted
        expense.serviceDescription = input["serviceDescription"] as? String
        expense.procedureCode = input["procedureCode"] as? String
        expense.diagnosisCode = input["diagnosisCode"] as? String
        expense.activeStatus = input["activeStatus"] as? Boolean ?: true
        expense.transactionId = (input["transactionId"] as? Int)?.toLong()
        expense.providerId = (input["providerId"] as? Int)?.toLong()
        expense.familyMemberId = (input["familyMemberId"] as? Int)?.toLong()
        (input["paidDate"] as? String)?.let { expense.paidDate = LocalDate.parse(it) }
        return expense
    }
}
