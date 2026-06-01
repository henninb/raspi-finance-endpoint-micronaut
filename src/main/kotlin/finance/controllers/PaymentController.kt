package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import jakarta.inject.Inject

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

    @Get(value = "/select/{paymentId}", produces = ["application/json"])
    fun selectByPaymentId(@PathVariable paymentId: Long): HttpResponse<Payment> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)
        return if (paymentOptional.isPresent) HttpResponse.ok(paymentOptional.get()) else HttpResponse.notFound()
    }

    @Put(value = "/update/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    fun updatePayment(@PathVariable paymentId: Long, @Body payment: Payment): HttpResponse<String> {
        payment.paymentId = paymentId
        val updated = paymentService.updatePayment(payment)
        return if (updated) HttpResponse.ok("payment updated") else HttpResponse.notFound()
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    @Delete(value = "/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): HttpResponse<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return HttpResponse.ok("payment deleted")
        }
        return HttpResponse.notFound("transaction not deleted: $paymentId")
    }
}