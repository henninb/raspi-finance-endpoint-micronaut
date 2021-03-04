package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

@Controller("/payment")
class PaymentController(@Inject val paymentService: PaymentService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

//    @Get("/select")
//    fun selectAllPayments(): MutableHttpResponse<MutableIterable<Payment>> {
//        println("payment select got here")
//        val payments = paymentService.findAllPayments()
//        println("payment select got there")
//        return HttpResponse.ok(payments)
//    }

    //@PostMapping(path = ["/insert"])
    @Post("/insert")
    fun insertPayment(@Body payment: Payment): HttpResponse<String> {
        paymentService.insertPayment(payment)
        return HttpResponse.ok("payment inserted")
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    //@DeleteMapping(path = ["/delete/{paymentId}"])
    @Delete("/delete/{paymentId}")
    fun deleteByPaymentId(@PathVariable paymentId: Long): HttpResponse<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        logger.info("deleteByPaymentId controller - $paymentId")
        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return HttpResponse.ok("payment deleted")
        }
        //throw EmptyAccountException("payment not deleted.")
        return HttpResponse.notModified()
    }
}