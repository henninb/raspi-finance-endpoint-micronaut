package finance.services

import finance.domain.ReceiptImage
import finance.helpers.ReceiptImageBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class ReceiptImageServiceSpec extends BaseServiceSpec {

    void 'test insertReceiptImage - valid receipt image'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        Set<ConstraintViolation<ReceiptImage>> constraintViolations = validator.validate(receiptImage)

        when:
        receiptImageService.insertReceiptImage(receiptImage)

        then:
        constraintViolations.size() == 0
        1 * validatorMock.validate(receiptImage) >> constraintViolations
        1 * receiptImageRepositoryMock.saveAndFlush(receiptImage) >> receiptImage
        0 * _
    }

    void 'test insertReceiptImage - validation failure throws ValidationException'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        ConstraintViolation<ReceiptImage> violation = Stub(ConstraintViolation)
        violation.getMessage() >> 'image is invalid'

        when:
        receiptImageService.insertReceiptImage(receiptImage)

        then:
        thrown(ValidationException)
        1 * validatorMock.validate(receiptImage) >> [violation].toSet()
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test findAllActive - returns list of receipt images'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        List<ReceiptImage> images = [receiptImage]

        when:
        List<ReceiptImage> results = receiptImageService.findAllActive()

        then:
        results.size() == 1
        1 * receiptImageRepositoryMock.findAll() >> images
        0 * _
    }

    void 'test findAllActive - returns empty list'() {
        when:
        List<ReceiptImage> results = receiptImageService.findAllActive()

        then:
        results.isEmpty()
        1 * receiptImageRepositoryMock.findAll() >> []
        0 * _
    }

    void 'test findByReceiptImageId - found'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        receiptImage.receiptImageId = 1L

        when:
        Optional<ReceiptImage> result = receiptImageService.findByReceiptImageId(1L)

        then:
        result.isPresent()
        1 * receiptImageRepositoryMock.findById(1L) >> Optional.of(receiptImage)
        0 * _
    }

    void 'test findByReceiptImageId - not found'() {
        when:
        Optional<ReceiptImage> result = receiptImageService.findByReceiptImageId(999L)

        then:
        !result.isPresent()
        1 * receiptImageRepositoryMock.findById(999L) >> Optional.empty()
        0 * _
    }

    void 'test updateReceiptImage - found and updated'() {
        given:
        ReceiptImage existing = ReceiptImageBuilder.builder().build()
        existing.receiptImageId = 1L
        ReceiptImage updated = ReceiptImageBuilder.builder().build()
        updated.receiptImageId = 1L

        when:
        Boolean result = receiptImageService.updateReceiptImage(updated)

        then:
        result
        1 * receiptImageRepositoryMock.findById(1L) >> Optional.of(existing)
        1 * receiptImageRepositoryMock.saveAndFlush(updated)
        0 * _
    }

    void 'test updateReceiptImage - not found returns false'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        receiptImage.receiptImageId = 999L

        when:
        Boolean result = receiptImageService.updateReceiptImage(receiptImage)

        then:
        !result
        1 * receiptImageRepositoryMock.findById(999L) >> Optional.empty()
        0 * _
    }

    void 'test deleteReceiptImage - deletes successfully'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        receiptImage.receiptImageId = 1L

        when:
        Boolean result = receiptImageService.deleteReceiptImage(receiptImage)

        then:
        result
        1 * receiptImageRepositoryMock.deleteById(1L)
        0 * _
    }
}
