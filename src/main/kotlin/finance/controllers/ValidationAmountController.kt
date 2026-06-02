package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.OwnerExtractorService
import finance.services.ValidationAmountService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import java.util.*

@Controller("/api/validation/amount")
class ValidationAmountController(
    private val validationAmountService: ValidationAmountService,
    private val ownerExtractorService: OwnerExtractorService,
) : BaseController() {

    @Get("/active")
    @Produces("application/json")
    fun selectAllActive(
        @QueryValue(defaultValue = "") accountNameOwner: String,
        @QueryValue(defaultValue = "") transactionState: String
    ): HttpResponse<List<ValidationAmount>> {
        if (accountNameOwner.isNotBlank() && transactionState.isNotBlank()) {
            val state = try {
                TransactionState.valueOf(
                    transactionState.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                )
            } catch (e: IllegalArgumentException) {
                return HttpResponse.badRequest()
            }
            val result = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner, state)
            return if (result.validationId == 0L) HttpResponse.ok(emptyList())
            else HttpResponse.ok(listOf(result))
        }
        val results = validationAmountService.findAllActive()
        return if (results.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(results)
    }

    @Get("/{validationId}")
    @Produces("application/json")
    fun selectById(@PathVariable validationId: Long): HttpResponse<ValidationAmount> {
        val result = validationAmountService.findById(validationId)
        return if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.notFound()
    }

    @Put("/{validationId}")
    @Consumes("application/json")
    @Produces("application/json")
    fun updateValidationAmount(
        @PathVariable validationId: Long,
        @Body validationAmount: ValidationAmount,
        request: HttpRequest<*>
    ): HttpResponse<ValidationAmount> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (validationAmount.owner.isNullOrBlank()) validationAmount.owner = owner
        val result = validationAmountService.updateValidationAmount(validationId, validationAmount)
        return if (result.isPresent) HttpResponse.ok(result.get())
        else throw HttpStatusException(HttpStatus.NOT_FOUND, "Validation amount not found: $validationId")
    }

    @Delete("/{validationId}")
    @Produces("application/json")
    fun deleteById(@PathVariable validationId: Long): HttpResponse<Void> {
        return if (validationAmountService.deleteById(validationId)) HttpResponse.noContent()
        else throw HttpStatusException(HttpStatus.NOT_FOUND, "Validation amount not found: $validationId")
    }

    @Post("/insert/{accountNameOwner}")
    @Consumes("application/json")
    @Produces("application/json")
    fun insertValidationAmount(
        @Body validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String,
        request: HttpRequest<*>
    ): HttpResponse<ValidationAmount> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (validationAmount.owner.isNullOrBlank()) validationAmount.owner = owner
        return try {
            val response = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)
            HttpResponse.status<ValidationAmount>(HttpStatus.CREATED).body(response)
        } catch (ex: HttpStatusException) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Failed to insert validation amount: ${ex.message}")
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }
    }

    @Get("/select/{accountNameOwner}/{transactionStateValue}")
    fun selectValidationAmountByAccountId(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        @PathVariable("transactionStateValue") transactionStateValue: String
    ): HttpResponse<ValidationAmount> {
        val newTransactionStateValue = transactionStateValue.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val validationAmount = validationAmountService.findValidationAmountByAccountNameOwner(
            accountNameOwner,
            TransactionState.valueOf(newTransactionStateValue)
        )
        return HttpResponse.ok(validationAmount)
    }
}
