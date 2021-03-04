package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import javax.inject.Inject

@Controller("/payment")
class PaymentController(@Inject val paymentService: PaymentService) {

    @Get(value = "/select", produces = ["application/json"])
    fun selectAllPayments(): HttpResponse<List<Payment>> {
        val payments = paymentService.findAllPayments()

        return HttpResponse.ok(payments)
    }

    @Post(value = "/insert", produces = ["application/json"])
    fun insertPayment(@Body payment: Payment): HttpResponse<String> {
        paymentService.insertPayment(payment)
        return HttpResponse.ok("payment inserted")
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    @Delete(value = "/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): HttpResponse<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        //logger.info("deleteByPaymentId controller - $paymentId")
        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return HttpResponse.ok("payment deleted")
        }
        //throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $paymentId")
        return HttpResponse.notFound("transaction not deleted: $paymentId")
    }
}