package finance.services

import finance.domain.Parameter
import finance.helpers.ParameterBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class ParameterServiceSpec extends BaseServiceSpec {

    void setup() {
    }

    void 'test - insert parameter'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        parameterService.insertParameter(parameter)

        then:
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter)
        0 * _
    }

    void 'test - insert parameter - parm not valid'() {
        given:
        Parameter parameter = ParameterBuilder.builder().withParameterName('').build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        parameterService.insertParameter(parameter)

        then:
        thrown(ValidationException)
        constraintViolations.size() == 1
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test - deleteByParameterName - invokes repository'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        parameterService.deleteByParameterName(parameter.parameterName)

        then:
        1 * parameterRepositoryMock.deleteByParameterName(parameter.parameterName)
        0 * _
    }

    void 'test - findAllActive - returns list'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        List<Parameter> results = parameterService.findAllActive()

        then:
        results.size() == 1
        1 * parameterRepositoryMock.findByActiveStatusOrderByParameterName(true) >> [parameter]
        0 * _
    }

    void 'test - findAllActive - returns empty list'() {
        when:
        List<Parameter> results = parameterService.findAllActive()

        then:
        results.isEmpty()
        1 * parameterRepositoryMock.findByActiveStatusOrderByParameterName(true) >> []
        0 * _
    }

    void 'test - findByParameter - found'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        Optional<Parameter> result = parameterService.findByParameter(parameter.parameterName)

        then:
        result.isPresent()
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.of(parameter)
        0 * _
    }

    void 'test - findByParameter - not found returns empty'() {
        when:
        Optional<Parameter> result = parameterService.findByParameter('nonexistent')

        then:
        !result.isPresent()
        1 * parameterRepositoryMock.findByParameterName('nonexistent') >> Optional.empty()
        0 * _
    }
}
