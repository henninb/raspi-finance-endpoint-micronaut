package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import java.util.*

@Controller("/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    // curl -k --header "Content-Type: application/json" --request POST --data '{"transactionDate": "2024-01-01", "amount": 100.00}' https://localhost:8443/validation/amount/insert/test_brian
    @Post("/insert/{accountNameOwner}")
    @Consumes("application/json")
    @Produces("application/json")
    fun insertValidationAmount(
        @Body validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String
    ): HttpResponse<ValidationAmount> {
        return try {
            val validationAmountResponse =
                validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

            logger.info("ValidationAmount inserted successfully")
            logger.info(mapper.writeValueAsString(validationAmountResponse))

            HttpResponse.ok(validationAmountResponse)
        } catch (ex: HttpStatusException) {
            logger.error("Failed to insert validation amount: ${ex.message}", ex)
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Failed to insert validation amount: ${ex.message}")
        } catch (ex: Exception) {
            logger.error("Unexpected error occurred while inserting validation amount: ${ex.message}", ex)
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }
    }

    // curl -k https://localhost:8443/validation/amount/select/test_brian/cleared
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
        logger.info(mapper.writeValueAsString(validationAmount))
        return HttpResponse.ok(validationAmount)
    }
}