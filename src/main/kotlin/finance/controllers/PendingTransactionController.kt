package finance.controllers

import finance.domain.PendingTransaction
import finance.services.OwnerExtractorService
import finance.services.PendingTransactionService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException

@Controller("/api/pending/transaction")
class PendingTransactionController(
    private val pendingTransactionService: PendingTransactionService,
    private val ownerExtractorService: OwnerExtractorService,
) : BaseController() {

    @Get("/active")
    @Produces("application/json")
    fun getAllPendingTransactions(): HttpResponse<List<PendingTransaction>> {
        val transactions = pendingTransactionService.getAllPendingTransactions()
        if (transactions.isEmpty()) throw HttpStatusException(HttpStatus.NOT_FOUND, "No pending transactions found.")
        return HttpResponse.ok(transactions)
    }

    @Get("/{pendingTransactionId}")
    @Produces("application/json")
    fun selectByPendingTransactionId(@PathVariable pendingTransactionId: Long): HttpResponse<PendingTransaction> {
        val result = pendingTransactionService.findByPendingTransactionId(pendingTransactionId)
        return if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.notFound()
    }

    @Post
    @Consumes("application/json")
    @Produces("application/json")
    fun insertPendingTransaction(@Body pendingTransaction: PendingTransaction, request: HttpRequest<*>): HttpResponse<PendingTransaction> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (pendingTransaction.owner.isNullOrBlank()) pendingTransaction.owner = owner
        return try {
            val response = pendingTransactionService.insertPendingTransaction(pendingTransaction)
            HttpResponse.status<PendingTransaction>(HttpStatus.CREATED).body(response)
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert pending transaction: ${ex.message}")
        }
    }

    @Put("/{pendingTransactionId}")
    @Consumes("application/json")
    @Produces("application/json")
    fun updatePendingTransaction(
        @PathVariable pendingTransactionId: Long,
        @Body pendingTransaction: PendingTransaction,
        request: HttpRequest<*>
    ): HttpResponse<PendingTransaction> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (pendingTransaction.owner.isNullOrBlank()) pendingTransaction.owner = owner
        val result = pendingTransactionService.updatePendingTransaction(pendingTransactionId, pendingTransaction)
        return if (result.isPresent) HttpResponse.ok(result.get())
        else throw HttpStatusException(HttpStatus.NOT_FOUND, "Pending transaction not found: $pendingTransactionId")
    }

    @Delete("/{pendingTransactionId}")
    fun deletePendingTransaction(@PathVariable pendingTransactionId: Long): HttpResponse<Void> {
        return if (pendingTransactionService.deletePendingTransaction(pendingTransactionId))
            HttpResponse.noContent()
        else throw HttpStatusException(HttpStatus.BAD_REQUEST, "Failed to delete pending transaction with ID: $pendingTransactionId")
    }

    @Delete("/delete/all")
    fun deleteAllPendingTransactions(): HttpResponse<Void> {
        return try {
            pendingTransactionService.deleteAllPendingTransactions()
            HttpResponse.noContent()
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete all pending transactions: ${ex.message}")
        }
    }
}
