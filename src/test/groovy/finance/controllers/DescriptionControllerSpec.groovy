package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.helpers.DescriptionBuilder
import finance.services.DescriptionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class DescriptionControllerSpec extends Specification {

    private DescriptionService descriptionServiceMock = GroovyMock(DescriptionService)
    private DescriptionController controller = new DescriptionController(descriptionServiceMock)

    void 'test selectAllDescriptions - returns 200 with all descriptions'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllDescriptions()

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.fetchAllDescriptions() >> [description]
        0 * _
    }

    void 'test selectAllDescriptions - returns 200 with empty list'() {
        when:
        HttpResponse response = controller.selectAllDescriptions()

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.fetchAllDescriptions() >> []
        0 * _
    }

    void 'test selectDescriptionName - returns 200 when found'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.selectDescriptionName('amazon.com')

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.findByDescriptionName('amazon.com') >> Optional.of(description)
        0 * _
    }

    void 'test selectDescriptionName - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectDescriptionName('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * descriptionServiceMock.findByDescriptionName('notfound') >> Optional.empty()
        0 * _
    }

    void 'test insertDescription - returns 201 on success'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.insertDescription(description)

        then:
        response.status == HttpStatus.CREATED
        1 * descriptionServiceMock.insertDescription(description)
        0 * _
    }

    void 'test updateDescription - returns 200 on success'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.updateDescription('amazon.com', description)

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.updateDescription(description) >> true
        0 * _
    }

    void 'test updateDescription - returns 404 when not found'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.updateDescription('notfound', description)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * descriptionServiceMock.updateDescription(description) >> false
        0 * _
    }

    void 'test deleteByDescriptionName - returns 200 when found'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByDescriptionName('amazon.com')

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.findByDescriptionName('amazon.com') >> Optional.of(description)
        1 * descriptionServiceMock.deleteByDescriptionName('amazon.com')
        0 * _
    }

    void 'test deleteByDescriptionName - returns 400 when not found'() {
        when:
        HttpResponse response = controller.deleteByDescriptionName('notfound')

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * descriptionServiceMock.findByDescriptionName('notfound') >> Optional.empty()
        0 * _
    }

    void 'test mergeDescriptions - returns 200 on success'() {
        given:
        Description description = DescriptionBuilder.builder().withDescription('target').build()
        MergeDescriptionsRequest request = new MergeDescriptionsRequest(['source1'], 'target')

        when:
        HttpResponse response = controller.mergeDescriptions(request)

        then:
        response.status == HttpStatus.OK
        1 * descriptionServiceMock.mergeDescriptions('target', ['source1'])
        1 * descriptionServiceMock.findByDescriptionName('target') >> Optional.of(description)
        0 * _
    }

    void 'test mergeDescriptions - returns 400 when blank target'() {
        given:
        MergeDescriptionsRequest request = new MergeDescriptionsRequest(['source1'], '')

        when:
        HttpResponse response = controller.mergeDescriptions(request)

        then:
        response.status == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test mergeDescriptions - returns 400 when empty sources'() {
        given:
        MergeDescriptionsRequest request = new MergeDescriptionsRequest([], 'target')

        when:
        HttpResponse response = controller.mergeDescriptions(request)

        then:
        response.status == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test mergeDescriptions - returns 400 on RuntimeException'() {
        given:
        MergeDescriptionsRequest request = new MergeDescriptionsRequest(['source1'], 'target')

        when:
        HttpResponse response = controller.mergeDescriptions(request)

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * descriptionServiceMock.mergeDescriptions('target', ['source1']) >> { throw new RuntimeException('merge failed') }
        0 * _
    }
}
