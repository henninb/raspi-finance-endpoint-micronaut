package finance.graphql

import finance.domain.ClaimStatus
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.MedicalExpenseService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.ReceiptImageService
import finance.services.TransactionService
import finance.services.TransferService
import finance.services.ValidationAmountService
import graphql.schema.DataFetcher
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager

@Singleton
class GraphQLQueryFetchers(
    @Inject private val accountService: AccountService,
    @Inject private val categoryService: CategoryService,
    @Inject private val descriptionService: DescriptionService,
    @Inject private val medicalExpenseService: MedicalExpenseService,
    @Inject private val parameterService: ParameterService,
    @Inject private val paymentService: PaymentService,
    @Inject private val transactionService: TransactionService,
    @Inject private val transferService: TransferService,
    @Inject private val receiptImageService: ReceiptImageService,
    @Inject private val validationAmountService: ValidationAmountService,
) {
    companion object {
        private val logger = LogManager.getLogger(GraphQLQueryFetchers::class.java)
    }

    fun accounts() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all accounts")
        accountService.accounts()
    }

    fun account() = DataFetcher { env ->
        val accountNameOwner = env.getArgument<String>("accountNameOwner")!!
        logger.info("GraphQL - fetching account: $accountNameOwner")
        accountService.findByAccountNameOwner(accountNameOwner).orElse(null)
    }

    fun transactions() = DataFetcher { env ->
        val accountNameOwner = env.getArgument<String>("accountNameOwner")!!
        logger.info("GraphQL - fetching transactions for: $accountNameOwner")
        transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)
    }

    fun transaction() = DataFetcher { env ->
        val guid = env.getArgument<String>("guid")!!
        logger.info("GraphQL - fetching transaction: $guid")
        transactionService.findTransactionByGuid(guid).orElse(null)
    }

    fun categories() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all categories")
        categoryService.fetchAllActiveCategories()
    }

    fun category() = DataFetcher { env ->
        val categoryName = env.getArgument<String>("categoryName")!!
        logger.info("GraphQL - fetching category: $categoryName")
        categoryService.findByCategoryName(categoryName).orElse(null)
    }

    fun descriptions() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all descriptions")
        descriptionService.fetchAllDescriptions()
    }

    fun description() = DataFetcher { env ->
        val descriptionName = env.getArgument<String>("descriptionName")!!
        logger.info("GraphQL - fetching description: $descriptionName")
        descriptionService.findByDescriptionName(descriptionName).orElse(null)
    }

    fun payments() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all payments")
        paymentService.findAllPayments()
    }

    fun payment() = DataFetcher { env ->
        val paymentId = env.getArgument<Int>("paymentId")!!.toLong()
        logger.info("GraphQL - fetching payment: $paymentId")
        paymentService.findByPaymentId(paymentId).orElse(null)
    }

    fun transfers() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all transfers")
        transferService.findAllTransfers()
    }

    fun transfer() = DataFetcher { env ->
        val transferId = env.getArgument<Int>("transferId")!!.toLong()
        logger.info("GraphQL - fetching transfer: $transferId")
        transferService.findByTransferId(transferId).orElse(null)
    }

    fun parameters() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all parameters")
        emptyList<Any>()
    }

    fun validationAmounts() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all validationAmounts")
        emptyList<Any>()
    }

    fun receiptImages() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all receipt images")
        receiptImageService.findAllActive()
    }

    fun medicalExpenses() = DataFetcher { _ ->
        logger.info("GraphQL - fetching all medical expenses")
        medicalExpenseService.findAllActive()
    }

    fun medicalExpense() = DataFetcher { env ->
        val medicalExpenseId = env.getArgument<Int>("medicalExpenseId")!!.toLong()
        logger.info("GraphQL - fetching medical expense: $medicalExpenseId")
        medicalExpenseService.findById(medicalExpenseId).orElse(null)
    }

    fun medicalExpensesByClaimStatus() = DataFetcher { env ->
        val claimStatusStr = env.getArgument<String>("claimStatus")!!
        logger.info("GraphQL - fetching medical expenses by claim status: $claimStatusStr")
        val claimStatus = ClaimStatus.valueOf(claimStatusStr)
        medicalExpenseService.findByClaimStatus(claimStatus)
    }
}
