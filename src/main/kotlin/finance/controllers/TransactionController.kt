package finance.controllers

import finance.domain.Account
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.services.TransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.math.BigDecimal
import java.util.*
import jakarta.inject.Inject

@Controller("/transaction")
class TransactionController(@Inject val transactionService: TransactionService) {


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
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): HttpResponse<String> {
        val results: Map<String, BigDecimal> = transactionService.fetchTotalsByAccountNameOwner(accountNameOwner)

        BaseController.logger.info("totals=${results}")

        return HttpResponse.ok(BaseController.mapper.writeValueAsString(results))
    }

    //curl -k https://hornsup:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @Get("/select/{guid}", produces = ["application/json"])
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
    @Put("/update/{guid}", produces = ["application/json"])
    //@Put("/update/{guid}"], consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @Body transaction: Map<String, Any>
    ): HttpResponse<String> {
        val toBePatchedTransaction = BaseController.mapper.convertValue(transaction, Transaction::class.java)
        val updateStatus: Boolean = transactionService.updateTransaction(toBePatchedTransaction)
        if (updateStatus) {
            return HttpResponse.ok("transaction updated")
        }
        //throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not found and thus not updated: $guid")
        return HttpResponse.notFound("transaction not found and thus not updated: $guid")
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
    ): HttpResponse<String> {
        val transactions = transactionService.updateTransactionState(guid, state)
        if (transactions.isNotEmpty()) {
            val response: MutableMap<String, String> = HashMap()
            response["message"] = "updated transactionState"
            response["transactions"] = transactions.toString()
            return HttpResponse.ok(BaseController.mapper.writeValueAsString(response))
        }
        //BaseController.logger.error("The transaction guid = $guid could not be updated for transaction state.")
        //meterService.incrementTransactionRestTransactionStateUpdateFailureCounter("unknown")
//        throw ResponseStatusException(
//            HttpStatus.NOT_MODIFIED,
//            "The transaction guid = $guid could not be updated for transaction state."
//        )
        return HttpResponse.notModified()
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

    //TODO: should return a 201 CREATED
    //curl -k --header "Content-Type: application/json" 'https://hornsup:8080/transaction/insert' -X POST -d ''
    @Post("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@Body transaction: Transaction): HttpResponse<String> {
        BaseController.logger.info("insert - transaction.transactionDate: $transaction")
        if (transactionService.insertTransaction(transaction)) {
            BaseController.logger.info(transaction.toString())
            return HttpResponse.ok("transaction inserted")
        }
        //throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not insert transaction.")
        return HttpResponse.badRequest("could not insert transaction.")
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
    @Delete("/delete/{guid}", produces = ["application/json"])
    fun deleteTransaction(@PathVariable("guid") guid: String): HttpResponse<String> {
        val transactionOption: Optional<Transaction> = transactionService.findTransactionByGuid(guid)
        if (transactionOption.isPresent) {
            if (transactionService.deleteTransactionByGuid(guid)) {
                return HttpResponse.ok("resource deleted")
            }
            return HttpResponse.notFound("transaction not deleted: $guid")
            //throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $guid")
        }
        return HttpResponse.notFound("transaction not deleted: $guid")
        //throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $guid")
    }

    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/payment/required
    @Get("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): HttpResponse<List<Account>> {
        return try {
            val accountNameOwners = transactionService.findAccountsThatRequirePayment()
            if (accountNameOwners.isEmpty()) {
                BaseController.logger.info("no accountNameOwners found.")
                return HttpResponse.notFound()
            }
            HttpResponse.ok(accountNameOwners)
        } catch (e: Exception) {
            BaseController.logger.error("Error finding accounts that require payment", e)
            HttpResponse.serverError("Internal server error")
        }
    }

}