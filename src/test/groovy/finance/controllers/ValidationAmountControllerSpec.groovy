package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.OwnerExtractorService
import finance.services.ValidationAmountService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Timestamp

@SuppressWarnings("GroovyAccessibility")
class ValidationAmountControllerSpec extends Specification {

    private ValidationAmountService validationAmountServiceMock = GroovyMock(ValidationAmountService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private ValidationAmountController controller = new ValidationAmountController(validationAmountServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    private ValidationAmount buildValidationAmount(Long id = 1L) {
        ValidationAmount va = new ValidationAmount()
        va.validationId = id
        va.accountId = 10L
        va.validationDate = new Timestamp(System.currentTimeMillis())
        va.transactionState = TransactionState.Cleared
        va.amount = new BigDecimal('1000.00')
        va.owner = 'brian'
        return va
    }

    void 'test selectAllActive - returns 200 with all active'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.selectAllActive('', '')

        then:
        response.status == HttpStatus.OK
        1 * validationAmountServiceMock.findAllActive() >> [va]
        0 * _
    }

    void 'test selectAllActive - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActive('', '')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * validationAmountServiceMock.findAllActive() >> []
        0 * _
    }

    void 'test selectAllActive - with accountNameOwner and state - returns result'() {
        given:
        ValidationAmount va = buildValidationAmount()
        va.validationId = 1L

        when:
        HttpResponse response = controller.selectAllActive('chase_brian', 'cleared')

        then:
        response.status == HttpStatus.OK
        1 * validationAmountServiceMock.findValidationAmountByAccountNameOwner('chase_brian', TransactionState.Cleared) >> va
        0 * _
    }

    void 'test selectAllActive - with invalid state returns 400'() {
        when:
        HttpResponse response = controller.selectAllActive('chase_brian', 'invalid_state')

        then:
        response.status == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test selectById - returns 200 when found'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.selectById(1L)

        then:
        response.status == HttpStatus.OK
        1 * validationAmountServiceMock.findById(1L) >> Optional.of(va)
        0 * _
    }

    void 'test selectById - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectById(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * validationAmountServiceMock.findById(999L) >> Optional.empty()
        0 * _
    }

    void 'test updateValidationAmount - returns 401 when no owner'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.updateValidationAmount(1L, va, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updateValidationAmount - returns 200 on success'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.updateValidationAmount(1L, va, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * validationAmountServiceMock.updateValidationAmount(1L, va) >> Optional.of(va)
        0 * _
    }

    void 'test updateValidationAmount - throws 404 when not found'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        controller.updateValidationAmount(999L, va, requestMock)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.NOT_FOUND
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * validationAmountServiceMock.updateValidationAmount(999L, va) >> Optional.empty()
        0 * _
    }

    void 'test deleteById - returns 204 when deleted'() {
        when:
        HttpResponse response = controller.deleteById(1L)

        then:
        response.status == HttpStatus.NO_CONTENT
        1 * validationAmountServiceMock.deleteById(1L) >> true
        0 * _
    }

    void 'test deleteById - throws 404 when not found'() {
        when:
        controller.deleteById(999L)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.NOT_FOUND
        1 * validationAmountServiceMock.deleteById(999L) >> false
        0 * _
    }

    void 'test insert - returns 401 when no owner'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.insert(va, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insert - returns 201 on success'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.insert(va, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * validationAmountServiceMock.save(va) >> va
        0 * _
    }

    void 'test insertValidationAmount - returns 401 when no owner'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.insertValidationAmount(va, 'chase_brian', requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertValidationAmount - returns 201 on success'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.insertValidationAmount(va, 'chase_brian', requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * validationAmountServiceMock.insertValidationAmount('chase_brian', va) >> va
        0 * _
    }

    void 'test selectValidationAmountByAccountId - returns 200'() {
        given:
        ValidationAmount va = buildValidationAmount()

        when:
        HttpResponse response = controller.selectValidationAmountByAccountId('chase_brian', 'cleared')

        then:
        response.status == HttpStatus.OK
        1 * validationAmountServiceMock.findValidationAmountByAccountNameOwner('chase_brian', TransactionState.Cleared) >> va
        0 * _
    }
}
