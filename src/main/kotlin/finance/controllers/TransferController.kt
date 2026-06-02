package finance.controllers

import finance.domain.Transfer
import finance.services.TransferService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import java.util.*

@Controller("/transfer")
class TransferController(private var transferService: TransferService) : BaseController() {

    // curl -k https://localhost:8443/transfer/select
    @Get("/select")
    @Produces("application/json")
    fun selectAllTransfers(): HttpResponse<List<Transfer>> {
        val transfers = transferService.findAllTransfers()

        return HttpResponse.ok(transfers)
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner": "test_brian", "transferAmount": 100.00, "description": "test transfer"}' https://localhost:8443/transfer/insert
    @Post("/insert")
    @Consumes("application/json")
    @Produces("application/json")
    fun insertTransfer(@Body transfer: Transfer): HttpResponse<Transfer> {
        return try {
            val transferResponse = transferService.insertTransfer(transfer)
            HttpResponse.ok(transferResponse)
        } catch (ex: HttpStatusException) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Failed to insert transfer: ${ex.message}")
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }
    }

    @Get("/select/{transferId}")
    @Produces("application/json")
    fun selectByTransferId(@PathVariable transferId: Long): HttpResponse<Transfer> {
        val result = transferService.findByTransferId(transferId)
        return if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.notFound()
    }

    @Put("/update/{transferId}")
    @Consumes("application/json")
    @Produces("application/json")
    fun updateTransfer(@PathVariable transferId: Long, @Body transfer: Transfer): HttpResponse<Transfer> {
        val result = transferService.updateTransfer(transferId, transfer)
        return if (result.isPresent) HttpResponse.ok(result.get())
        else throw HttpStatusException(HttpStatus.NOT_FOUND, "Transfer not found: $transferId")
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/transfer/delete/1001
    @Delete("/delete/{transferId}")
    @Produces("application/json")
    fun deleteByTransferId(@PathVariable transferId: Long): HttpResponse<Transfer> {
        val transferOptional: Optional<Transfer> = transferService.findByTransferId(transferId)

        if (transferOptional.isPresent) {
            val transfer: Transfer = transferOptional.get()
            logger.info("transfer deleted: ${transfer.transferId}")
            transferService.deleteByTransferId(transferId)
            return HttpResponse.ok(transfer)
        }
        throw HttpStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $transferId")
    }
}