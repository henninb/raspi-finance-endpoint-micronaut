package finance.controllers

import finance.domain.Payment
import finance.helpers.PaymentBuilder
import finance.services.OwnerExtractorService
import finance.services.PaymentService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class PaymentControllerSpec extends Specification {

    private PaymentService paymentServiceMock = GroovyMock(PaymentService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private PaymentController controller = new PaymentController(paymentServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    void 'test selectAllPayments - returns 200 with payments'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllPayments()

        then:
        response.status == HttpStatus.OK
        1 * paymentServiceMock.findAllPayments() >> [payment]
        0 * _
    }

    void 'test selectAllPayments - returns 200 with empty list'() {
        when:
        HttpResponse response = controller.selectAllPayments()

        then:
        response.status == HttpStatus.OK
        1 * paymentServiceMock.findAllPayments() >> []
        0 * _
    }

    void 'test selectByPaymentId - returns 200 when found'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.selectByPaymentId(1L)

        then:
        response.status == HttpStatus.OK
        1 * paymentServiceMock.findByPaymentId(1L) >> Optional.of(payment)
        0 * _
    }

    void 'test selectByPaymentId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectByPaymentId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * paymentServiceMock.findByPaymentId(999L) >> Optional.empty()
        0 * _
    }

    void 'test insertPayment - returns 401 when no owner'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.insertPayment(payment, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertPayment - returns 201 on success'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.insertPayment(payment, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * paymentServiceMock.insertPayment(payment)
        0 * _
    }

    void 'test updatePayment - returns 401 when no owner'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.updatePayment(1L, payment, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updatePayment - returns 200 on success'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.updatePayment(1L, payment, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * paymentServiceMock.updatePayment(payment) >> true
        0 * _
    }

    void 'test updatePayment - returns 404 when not found'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.updatePayment(1L, payment, requestMock)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * paymentServiceMock.updatePayment(payment) >> false
        0 * _
    }

    void 'test deleteByPaymentId - returns 200 when found'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByPaymentId(1L)

        then:
        response.status == HttpStatus.OK
        1 * paymentServiceMock.findByPaymentId(1L) >> Optional.of(payment)
        1 * paymentServiceMock.deleteByPaymentId(1L)
        0 * _
    }

    void 'test deleteByPaymentId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.deleteByPaymentId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * paymentServiceMock.findByPaymentId(999L) >> Optional.empty()
        0 * _
    }
}
