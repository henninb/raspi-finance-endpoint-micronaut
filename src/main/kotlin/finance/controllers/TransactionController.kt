package finance.controllers

import finance.domain.Account
import finance.domain.BonusProgress
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.services.OwnerExtractorService
import finance.services.TransactionService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import jakarta.inject.Inject

@Controller("/api/transaction")
class TransactionController(
    @Inject val transactionService: TransactionService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    private fun extractOwner(request: HttpRequest<*>) = ownerExtractorService.extractOwner(request)


    //curl https://hornsup:8080/transaction/account/select/usbankcash_brian
    @Get("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): HttpResponse<List<Transaction>> {
        val transactions: List<Transaction> =
            transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)
        if (transactions.isEmpty()) {
            //BaseController.logger.error("transactions.size=${transactions.size}")
            //TODO: not found, should I take this action?
            //HttpResponse.notFound().build<List<Transaction>>()
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(transactions)
    }

    //transaction-management/
    //accounts/{accountNameOwner}/transactions/totals
    //curl -k https://hornsup:8080/transaction/account/totals/chase_brian
    @Get("/account/totals/{accountNameOwner}", produces = ["application/json"])
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): HttpResponse<Map<String, BigDecimal>> {
        val results: Map<String, BigDecimal> = transactionService.fetchTotalsByAccountNameOwner(accountNameOwner)
        BaseController.logger.info("totals=${results}")
        return HttpResponse.ok(results)
    }

    //curl -k https://hornsup:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @Get("/{guid}", produces = ["application/json"])
    fun findTransaction(@PathVariable("guid") guid: String): HttpResponse<Transaction> {
        BaseController.logger.debug("findTransaction() guid = $guid")
        val transactionOption: Optional<Transaction> = transactionService.findTransactionByGuid(guid)
        if (transactionOption.isPresent) {
            val transaction: Transaction = transactionOption.get()
            return HttpResponse.ok(transaction)
        }

        //BaseController.logger.error("Transaction not found, guid = $guid")
        //meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
        //throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found, guid: $guid")
        return HttpResponse.notFound()
    }

    //TODO: 2021-01-10, return the payload of the updated and the inserted
    //TODO: 2021-01-10, consumes JSON should be turned back on
    @Put("/{guid}", produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @Body transaction: Map<String, Any>,
        request: HttpRequest<*>
    ): HttpResponse<Transaction> {
        val toBePatchedTransaction = BaseController.mapper.convertValue(transaction, Transaction::class.java)
        if (toBePatchedTransaction.owner.isNullOrBlank()) {
            toBePatchedTransaction.owner = extractOwner(request) ?: ""
        }
        return if (transactionService.updateTransaction(toBePatchedTransaction))
            HttpResponse.ok(toBePatchedTransaction)
        else HttpResponse.notFound()
    }

    //TODO: return the payload of the updated and the inserted
    @Put(
        "/state/update/{guid}/{state}",
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("state") state: TransactionState
    ): HttpResponse<Transaction> {
        val transactions = transactionService.updateTransactionState(guid, state)
        return if (transactions.isNotEmpty()) HttpResponse.ok(transactions.first()) else HttpResponse.notModified()
    }

    @Put(
        "/reoccurring/update/{guid}/{reoccurring}",
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionReoccurringState(
        @PathVariable("guid") guid: String,
        @PathVariable("reoccurring") reoccurring: Boolean
    ): HttpResponse<String> {
        val updateStatus: Boolean = transactionService.updateTransactionReoccurringFlag(guid, reoccurring)
        if (updateStatus) {
            return HttpResponse.ok("transaction reoccurring updated")
        }
        //BaseController.logger.error("The transaction guid = $guid could not be updated for reoccurring state.")
        //meterService.incrementTransactionRestReoccurringStateUpdateFailureCounter("unknown")
        //throw ResponseStatusException(HttpStatus.NOT_MODIFIED, "could not updated transaction for reoccurring state.")
        return HttpResponse.notModified()
    }

    @Post(consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@Body transaction: Transaction, request: HttpRequest<*>): HttpResponse<Transaction> {
        val owner = extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (transaction.owner.isNullOrBlank()) {
            transaction.owner = owner
        }
        BaseController.logger.info("insert - transaction.transactionDate: $transaction")
        return if (transactionService.insertTransaction(transaction)) {
            HttpResponse.status<Transaction>(HttpStatus.CREATED).body(transaction)
        } else {
            HttpResponse.badRequest()
        }
    }

    // change the account name owner of a given transaction
    @Put("/update/account", consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(@Body payload: Map<String, String>): HttpResponse<String> {
        //TODO: need to complete action
        BaseController.logger.info("value of accountNameOwner: " + payload["accountNameOwner"])
        BaseController.logger.info("value of guid: " + payload["guid"])
        transactionService.changeAccountNameOwner(payload)
        BaseController.logger.info("transaction account updated")

        return HttpResponse.ok("transaction account updated")
    }

    // curl -k -X PUT 'https://hornsup:8080/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f'  --header "Content-Type: application/json" -d 'test'
    @Put("/update/receipt/image/{guid}", produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @Body payload: String
    ): HttpResponse<ReceiptImage> {
        val receiptImage = transactionService.updateTransactionReceiptImageByGuid(guid, payload)
        BaseController.logger.info("set transaction receipt image for guid = $guid")
        //return _root_ide_package_.io.micronaut.http.HttpResponse.ok("transaction receipt image updated")
        return HttpResponse.ok(receiptImage)
    }

    //curl -k --header "Content-Type: application/json" -X DELETE 'https://hornsup:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a'
    @Delete("/{guid}", produces = ["application/json"])
    fun deleteTransaction(@PathVariable("guid") guid: String): HttpResponse<Transaction> {
        val transactionOption: Optional<Transaction> = transactionService.findTransactionByGuid(guid)
        if (transactionOption.isPresent) {
            return if (transactionService.deleteTransactionByGuid(guid))
                HttpResponse.ok(transactionOption.get())
            else HttpResponse.notFound()
        }
        return HttpResponse.notFound()
    }

    @Get("/active", produces = ["application/json"])
    fun selectAllActiveTransactions(): HttpResponse<List<Transaction>> {
        val transactions = transactionService.findAllActiveTransactions()
        if (transactions.isEmpty()) {
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(transactions)
    }

    @Get("/category/{categoryName}", produces = ["application/json"])
    fun selectTransactionsByCategory(@PathVariable categoryName: String): HttpResponse<List<Transaction>> {
        val transactions = transactionService.findTransactionsByCategory(categoryName)
        if (transactions.isEmpty()) {
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(transactions)
    }

    @Get("/description/{descriptionName}", produces = ["application/json"])
    fun selectTransactionsByDescription(@PathVariable descriptionName: String): HttpResponse<List<Transaction>> {
        val transactions = transactionService.findTransactionsByDescription(descriptionName)
        if (transactions.isEmpty()) {
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(transactions)
    }

    @Get("/date-range", produces = ["application/json"])
    fun selectTransactionsByDateRange(
        @QueryValue startDate: LocalDate,
        @QueryValue endDate: LocalDate
    ): HttpResponse<List<Transaction>> {
        val transactions = transactionService.findTransactionsByDateRange(startDate, endDate)
        if (transactions.isEmpty()) {
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(transactions)
    }

    @Get("/account/select/{accountNameOwner}/paged", produces = ["application/json"])
    fun selectByAccountNameOwnerPaged(
        @PathVariable accountNameOwner: String,
        pageable: Pageable
    ): HttpResponse<Page<Transaction>> {
        val page = transactionService.findByAccountNameOwnerPaged(accountNameOwner, pageable)
        return HttpResponse.ok(page)
    }

    @Get("/account/bonus-progress/{accountNameOwner}", produces = ["application/json"])
    fun getBonusProgress(
        @PathVariable accountNameOwner: String,
        @QueryValue startDate: LocalDate,
        @QueryValue targetAmount: BigDecimal,
        @QueryValue bonusAmount: BigDecimal,
        @QueryValue(defaultValue = "90") windowDays: Long
    ): HttpResponse<BonusProgress> {
        val progress = transactionService.getBonusProgress(accountNameOwner, startDate, targetAmount, bonusAmount, windowDays)
        return HttpResponse.ok(progress)
    }

    @Post("/future", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(@Body transaction: Transaction): HttpResponse<Transaction> {
        val future = transactionService.insertFutureTransaction(transaction)
        return HttpResponse.ok(future)
    }

    @Delete("/receipt/image/{guid}", produces = ["application/json"])
    fun deleteTransactionReceiptImageByGuid(@PathVariable guid: String): HttpResponse<String> {
        return if (transactionService.deleteReceiptImageForTransactionByGuid(guid)) {
            HttpResponse.ok("receipt image deleted")
        } else {
            HttpResponse.notFound("no receipt image found for guid: $guid")
        }
    }

    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/payment/required
    @Get("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): HttpResponse<List<Account>> {
        return try {
            val accountNameOwners = transactionService.findAccountsThatRequirePayment()
            if (accountNameOwners.isEmpty()) {
                BaseController.logger.info("no accountNameOwners found.")
                return HttpResponse.notFound<List<Account>>()
            }
            HttpResponse.ok(accountNameOwners)
        } catch (e: Exception) {
            BaseController.logger.error("Error finding accounts that require payment", e)
            HttpResponse.serverError<List<Account>>()
        }
    }

}