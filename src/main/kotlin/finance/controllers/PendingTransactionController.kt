package finance.controllers

import finance.domain.PendingTransaction
import finance.services.PendingTransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException

@Controller("/pending/transaction")
class PendingTransactionController(private val pendingTransactionService: PendingTransactionService) : BaseController() {

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner": "test_brian", "description": "pending transaction", "amount": 50.00}' https://localhost:8443/pending/transaction/insert
    @Post("/insert")
    @Consumes("application/json")
    @Produces("application/json")
    fun insertPendingTransaction(@Body pendingTransaction: PendingTransaction): HttpResponse<PendingTransaction> {
        return try {
            val response = pendingTransactionService.insertPendingTransaction(pendingTransaction)
            HttpResponse.ok(response)
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert pending transaction: ${ex.message}")
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/pending/transaction/delete/1
    @Delete("/delete/{id}")
    fun deletePendingTransaction(@PathVariable id: Long): HttpResponse<Void> {
        return if (pendingTransactionService.deletePendingTransaction(id)) {
            HttpResponse.noContent()
        } else {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Failed to delete pending transaction with ID: $id")
        }
    }

    // curl -k https://localhost:8443/pending/transaction/all
    @Get("/all")
    @Produces("application/json")
    fun getAllPendingTransactions(): HttpResponse<List<PendingTransaction>> {
        val transactions = pendingTransactionService.getAllPendingTransactions()
        if (transactions.isEmpty()) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "No pending transactions found.")
        }
        return HttpResponse.ok(transactions)
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/pending/transaction/delete/all
    @Delete("/delete/all")
    fun deleteAllPendingTransactions(): HttpResponse<Void> {
        return try {
            pendingTransactionService.deleteAllPendingTransactions()
            HttpResponse.noContent()
        } catch (ex: Exception) {
            throw HttpStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete all pending transactions: ${ex.message}"
            )
        }
    }
}