package finance.controllers

import finance.domain.ReceiptImage
import finance.helpers.ReceiptImageBuilder
import finance.services.OwnerExtractorService
import finance.services.ReceiptImageService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class ReceiptImageControllerSpec extends Specification {

    private ReceiptImageService receiptImageServiceMock = GroovyMock(ReceiptImageService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private ReceiptImageController controller = new ReceiptImageController(receiptImageServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    void 'test selectAllActive - returns 200 with receipt images'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.OK
        1 * receiptImageServiceMock.findAllActive() >> [receiptImage]
        0 * _
    }

    void 'test selectAllActive - returns 200 with empty list'() {
        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.OK
        1 * receiptImageServiceMock.findAllActive() >> []
        0 * _
    }

    void 'test selectById - returns 200 when found'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.selectById(1L)

        then:
        response.status == HttpStatus.OK
        1 * receiptImageServiceMock.findByReceiptImageId(1L) >> Optional.of(receiptImage)
        0 * _
    }

    void 'test selectById - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectById(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * receiptImageServiceMock.findByReceiptImageId(999L) >> Optional.empty()
        0 * _
    }

    void 'test insertReceiptImage - returns 401 when no owner'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.insertReceiptImage(receiptImage, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertReceiptImage - returns 201 on success'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.insertReceiptImage(receiptImage, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * receiptImageServiceMock.insertReceiptImage(receiptImage)
        0 * _
    }

    void 'test updateReceiptImage - returns 401 when no owner'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.updateReceiptImage(1L, receiptImage, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updateReceiptImage - returns 200 on success'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.updateReceiptImage(1L, receiptImage, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * receiptImageServiceMock.updateReceiptImage(receiptImage) >> true
        0 * _
    }

    void 'test updateReceiptImage - returns 404 when not found'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.updateReceiptImage(1L, receiptImage, requestMock)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * receiptImageServiceMock.updateReceiptImage(receiptImage) >> false
        0 * _
    }

    void 'test deleteByReceiptImageId - returns 200 when found'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByReceiptImageId(1L)

        then:
        response.status == HttpStatus.OK
        1 * receiptImageServiceMock.findByReceiptImageId(1L) >> Optional.of(receiptImage)
        1 * receiptImageServiceMock.deleteReceiptImage(receiptImage)
        0 * _
    }

    void 'test deleteByReceiptImageId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.deleteByReceiptImageId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * receiptImageServiceMock.findByReceiptImageId(999L) >> Optional.empty()
        0 * _
    }
}
