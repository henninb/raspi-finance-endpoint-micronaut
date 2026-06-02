package finance.controllers

import finance.domain.ReceiptImage
import finance.services.OwnerExtractorService
import finance.services.ReceiptImageService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject

@Controller("/api/receipt/image")
class ReceiptImageController(
    @Inject val receiptImageService: ReceiptImageService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<ReceiptImage>> =
        HttpResponse.ok(receiptImageService.findAllActive())

    @Get("/{receiptImageId}", produces = ["application/json"])
    fun selectById(@PathVariable receiptImageId: Long): HttpResponse<ReceiptImage> {
        val optional = receiptImageService.findByReceiptImageId(receiptImageId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(consumes = ["application/json"], produces = ["application/json"])
    fun insertReceiptImage(@Body receiptImage: ReceiptImage, request: HttpRequest<*>): HttpResponse<ReceiptImage> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (receiptImage.owner.isNullOrBlank()) receiptImage.owner = owner
        receiptImageService.insertReceiptImage(receiptImage)
        return HttpResponse.status<ReceiptImage>(HttpStatus.CREATED).body(receiptImage)
    }

    @Put("/{receiptImageId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateReceiptImage(@PathVariable receiptImageId: Long, @Body receiptImage: ReceiptImage, request: HttpRequest<*>): HttpResponse<ReceiptImage> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        receiptImage.receiptImageId = receiptImageId
        if (receiptImage.owner.isNullOrBlank()) receiptImage.owner = owner
        return if (receiptImageService.updateReceiptImage(receiptImage)) HttpResponse.ok(receiptImage) else HttpResponse.notFound()
    }

    @Delete("/{receiptImageId}", produces = ["application/json"])
    fun deleteByReceiptImageId(@PathVariable receiptImageId: Long): HttpResponse<ReceiptImage> {
        val optional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (optional.isPresent) {
            receiptImageService.deleteReceiptImage(optional.get())
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }
}
