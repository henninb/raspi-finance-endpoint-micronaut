package finance.controllers

import finance.services.ReceiptImageService
import io.micronaut.http.annotation.Controller
import jakarta.inject.Inject

@Controller("/receipt/image")
class ReceiptImageController(@Inject val receiptImageService: ReceiptImageService) {
}