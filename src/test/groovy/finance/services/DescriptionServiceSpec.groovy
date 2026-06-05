package finance.services

import finance.domain.Description
import finance.helpers.DescriptionBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class DescriptionServiceSpec extends BaseServiceSpec {
    void setup() {
    }

    void 'test - insert description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        descriptionService.insertDescription(description)

        then:
        1 * validatorMock.validate(description) >> constraintViolations
        1 * descriptionRepositoryMock.saveAndFlush(description)
        0 * _
    }

    void 'test - insert description - empty descriptionName'() {
        given:
        Description description = DescriptionBuilder.builder().withDescription('').build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        descriptionService.insertDescription(description)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(description) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test - deleteByDescriptionName - invokes repository'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        Boolean result = descriptionService.deleteByDescriptionName(description.descriptionName)

        then:
        result
        1 * descriptionRepositoryMock.deleteByDescriptionName(description.descriptionName)
        0 * _
    }

    void 'test - findByDescriptionName - found'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        Optional<Description> result = descriptionService.findByDescriptionName(description.descriptionName)

        then:
        result.isPresent()
        1 * descriptionRepositoryMock.findByDescriptionName(description.descriptionName) >> Optional.of(description)
        0 * _
    }

    void 'test - findByDescriptionName - not found'() {
        when:
        Optional<Description> result = descriptionService.findByDescriptionName('nonexistent')

        then:
        !result.isPresent()
        1 * descriptionRepositoryMock.findByDescriptionName('nonexistent') >> Optional.empty()
        0 * _
    }

    void 'test - findByOwnerAndDescriptionName - found'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        Optional<Description> result = descriptionService.findByOwnerAndDescriptionName('brian', description.descriptionName)

        then:
        result.isPresent()
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName('brian', description.descriptionName) >> Optional.of(description)
        0 * _
    }

    void 'test - fetchAllDescriptions - empty list'() {
        when:
        List<Description> results = descriptionService.fetchAllDescriptions()

        then:
        results.isEmpty()
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> []
        0 * _
    }

    void 'test - fetchAllDescriptions - with descriptions and counts'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        List<Object[]> countRows = [['foo', 7L] as Object[]]

        when:
        List<Description> results = descriptionService.fetchAllDescriptions()

        then:
        results.size() == 1
        results[0].descriptionCount == 7L
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> [description]
        1 * transactionRepositoryMock.countByDescriptionNameIn(['foo']) >> countRows
        0 * _
    }

    void 'test - updateDescription - found and updated'() {
        given:
        Description existing = DescriptionBuilder.builder().withActiveStatus(true).build()
        Description updated = DescriptionBuilder.builder().withActiveStatus(false).build()

        when:
        Boolean result = descriptionService.updateDescription(updated)

        then:
        result
        1 * descriptionRepositoryMock.findByDescriptionName(updated.descriptionName) >> Optional.of(existing)
        1 * descriptionRepositoryMock.saveAndFlush(existing)
        0 * _
    }

    void 'test - updateDescription - not found returns false'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        Boolean result = descriptionService.updateDescription(description)

        then:
        !result
        1 * descriptionRepositoryMock.findByDescriptionName(description.descriptionName) >> Optional.empty()
        0 * _
    }

    void 'test - mergeDescriptions - success with single source'() {
        given:
        Description target = DescriptionBuilder.builder().withDescription('target_desc').build()
        Description source = DescriptionBuilder.builder().withDescription('source_desc').build()

        when:
        Description result = descriptionService.mergeDescriptions('target_desc', ['source_desc'])

        then:
        result.descriptionName == 'target_desc'
        1 * descriptionRepositoryMock.findByDescriptionName('target_desc') >> Optional.of(target)
        1 * descriptionRepositoryMock.findByDescriptionName('source_desc') >> Optional.of(source)
        1 * transactionRepositoryMock.bulkUpdateDescription('source_desc', 'target_desc') >> 3
        1 * descriptionRepositoryMock.saveAndFlush(source)
        0 * _
    }

    void 'test - mergeDescriptions - target not found throws RuntimeException'() {
        when:
        descriptionService.mergeDescriptions('nonexistent', ['source_desc'])

        then:
        thrown(RuntimeException)
        1 * descriptionRepositoryMock.findByDescriptionName('nonexistent') >> Optional.empty()
        0 * _
    }

    void 'test - mergeDescriptions - source not found skips that source'() {
        given:
        Description target = DescriptionBuilder.builder().withDescription('target_desc').build()

        when:
        Description result = descriptionService.mergeDescriptions('target_desc', ['nonexistent_source'])

        then:
        result.descriptionName == 'target_desc'
        1 * descriptionRepositoryMock.findByDescriptionName('target_desc') >> Optional.of(target)
        1 * descriptionRepositoryMock.findByDescriptionName('nonexistent_source') >> Optional.empty()
        0 * _
    }
}
