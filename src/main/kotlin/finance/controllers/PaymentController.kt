package finance.controllers

import finance.domain.Payment
import finance.services.OwnerExtractorService
import finance.services.PaymentService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/payment")
class PaymentController(
    @Inject val paymentService: PaymentService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active/paged", produces = ["application/json"])
    fun selectAllPaymentsPaged(request: HttpRequest<*>, pageable: Pageable): HttpResponse<Page<Payment>> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        return HttpResponse.ok(paymentService.findAllPaymentsPaged(owner, pageable))
    }

    @Get("/active", produces = ["application/json"])
    fun selectAllPayments(request: HttpRequest<*>): HttpResponse<List<Payment>> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        return HttpResponse.ok(paymentService.findAllPayments(owner))
    }

    @Get("/{paymentId}", produces = ["application/json"])
    fun selectByPaymentId(@PathVariable paymentId: Long, request: HttpRequest<*>): HttpResponse<Payment> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional = paymentService.findByOwnerAndPaymentId(owner, paymentId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertPayment(@Body payment: Payment, request: HttpRequest<*>): HttpResponse<Payment> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (payment.owner.isBlank()) payment.owner = owner
        paymentService.insertPayment(payment)
        return HttpResponse.status<Payment>(HttpStatus.CREATED).body(payment)
    }

    @Put("/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    fun updatePayment(@PathVariable paymentId: Long, @Body payment: Payment, request: HttpRequest<*>): HttpResponse<Payment> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        payment.paymentId = paymentId
        if (payment.owner.isBlank()) payment.owner = owner
        return if (paymentService.updatePayment(payment)) HttpResponse.ok(payment) else HttpResponse.notFound()
    }

    @Delete("/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long, request: HttpRequest<*>): HttpResponse<Payment> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional = paymentService.findByOwnerAndPaymentId(owner, paymentId)
        if (optional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }
}
