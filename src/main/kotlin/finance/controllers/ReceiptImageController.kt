package finance.controllers

import finance.services.ParameterService
import finance.services.ReceiptImageService
import io.micronaut.http.annotation.Controller
import javax.inject.Inject

@Controller("/receipt/image")
class ReceiptImageController(@Inject val receiptImageService: ReceiptImageService) {
}