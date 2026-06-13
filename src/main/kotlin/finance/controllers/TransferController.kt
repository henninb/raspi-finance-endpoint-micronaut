package finance.controllers

import finance.domain.Transfer
import finance.exceptions.DuplicateTransferException
import finance.services.TransferService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import java.util.*

@Controller("/api/transfer")
class TransferController(private var transferService: TransferService) : BaseController() {

    // curl -k https://localhost:8443/transfer/select
    @Get("/active")
    @Produces("application/json")
    fun selectAllTransfers(): HttpResponse<List<Transfer>> {
        val transfers = transferService.findAllTransfers()

        return HttpResponse.ok(transfers)
    }

    @Get("/active/paged")
    @Produces("application/json")
    fun selectAllTransfersPaged(pageable: Pageable): HttpResponse<Page<Transfer>> {
        val page = transferService.findAllTransfersPaged(pageable)
        return HttpResponse.ok(page)
    }

    @Post
    @Consumes("application/json")
    @Produces("application/json")
    fun insertTransfer(@Body transfer: Transfer): HttpResponse<Transfer> {
        return try {
            val transferResponse = transferService.insertTransfer(transfer)
            HttpResponse.status<Transfer>(HttpStatus.CREATED).body(transferResponse)
        } catch (ex: DuplicateTransferException) {
            throw HttpStatusException(HttpStatus.CONFLICT, ex.message)
        } catch (ex: HttpStatusException) {
            throw ex
        } catch (ex: RuntimeException) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, ex.message)
        } catch (ex: Exception) {
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }
    }

    @Get("/{transferId}")
    @Produces("application/json")
    fun selectByTransferId(@PathVariable transferId: Long): HttpResponse<Transfer> {
        val result = transferService.findByTransferId(transferId)
        return if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.notFound()
    }

    @Put("/{transferId}")
    @Consumes("application/json")
    @Produces("application/json")
    fun updateTransfer(@PathVariable transferId: Long, @Body transfer: Transfer): HttpResponse<Transfer> {
        val result = transferService.updateTransfer(transferId, transfer)
        return if (result.isPresent) HttpResponse.ok(result.get())
        else throw HttpStatusException(HttpStatus.NOT_FOUND, "Transfer not found: $transferId")
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/transfer/delete/1001
    @Delete("/{transferId}")
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