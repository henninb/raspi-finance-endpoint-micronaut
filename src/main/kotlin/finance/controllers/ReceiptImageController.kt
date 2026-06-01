package finance.controllers

import finance.domain.ReceiptImage
import finance.services.ReceiptImageService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import jakarta.inject.Inject

@Controller("/receipt/image")
class ReceiptImageController(@Inject val receiptImageService: ReceiptImageService) {

    @Get("/select/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<ReceiptImage>> {
        val images = receiptImageService.findAllActive()
        return HttpResponse.ok(images)
    }

    @Get("/select/{receiptImageId}", produces = ["application/json"])
    fun selectById(@PathVariable receiptImageId: Long): HttpResponse<ReceiptImage> {
        val optional = receiptImageService.findByReceiptImageId(receiptImageId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertReceiptImage(@Body receiptImage: ReceiptImage): HttpResponse<String> {
        receiptImageService.insertReceiptImage(receiptImage)
        return HttpResponse.ok("receipt image inserted")
    }

    @Put("/update/{receiptImageId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateReceiptImage(@PathVariable receiptImageId: Long, @Body receiptImage: ReceiptImage): HttpResponse<String> {
        receiptImage.receiptImageId = receiptImageId
        val updated = receiptImageService.updateReceiptImage(receiptImage)
        return if (updated) HttpResponse.ok("receipt image updated") else HttpResponse.notFound()
    }

    @Delete("/delete/{receiptImageId}", produces = ["application/json"])
    fun deleteByReceiptImageId(@PathVariable receiptImageId: Long): HttpResponse<String> {
        val optional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (optional.isPresent) {
            receiptImageService.deleteReceiptImage(optional.get())
            return HttpResponse.ok("receipt image deleted")
        }
        return HttpResponse.notFound()
    }
}
