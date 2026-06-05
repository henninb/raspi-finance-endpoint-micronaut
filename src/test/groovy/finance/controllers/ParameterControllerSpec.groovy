package finance.controllers

import finance.domain.Parameter
import finance.helpers.ParameterBuilder
import finance.services.OwnerExtractorService
import finance.services.ParameterService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class ParameterControllerSpec extends Specification {

    private ParameterService parameterServiceMock = GroovyMock(ParameterService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private ParameterController controller = new ParameterController(parameterServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    void 'test selectAllActive - returns 200 with parameters'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.OK
        1 * parameterServiceMock.findAllActive() >> [parameter]
        0 * _
    }

    void 'test selectAllActive - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * parameterServiceMock.findAllActive() >> []
        0 * _
    }

    void 'test selectParameter - returns 200 when found'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        HttpResponse response = controller.selectParameter('payment_account')

        then:
        response.status == HttpStatus.OK
        1 * parameterServiceMock.findByParameter('payment_account') >> Optional.of(parameter)
        0 * _
    }

    void 'test selectParameter - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectParameter('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * parameterServiceMock.findByParameter('notfound') >> Optional.empty()
        0 * _
    }

    void 'test insertParameter - returns 401 when no owner'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        HttpResponse response = controller.insertParameter(parameter, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertParameter - returns 201 on success'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        HttpResponse response = controller.insertParameter(parameter, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * parameterServiceMock.insertParameter(parameter)
        0 * _
    }

    void 'test deleteByParameterName - returns 200 when found'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByParameterName('payment_account')

        then:
        response.status == HttpStatus.OK
        1 * parameterServiceMock.findByParameter('payment_account') >> Optional.of(parameter)
        1 * parameterServiceMock.deleteByParameterName('payment_account')
        0 * _
    }

    void 'test deleteByParameterName - returns 404 when not found'() {
        when:
        HttpResponse response = controller.deleteByParameterName('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * parameterServiceMock.findByParameter('notfound') >> Optional.empty()
        0 * _
    }
}
