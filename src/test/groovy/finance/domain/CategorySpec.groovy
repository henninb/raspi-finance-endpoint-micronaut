package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.CategoryBuilder
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

class CategorySpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()
    protected String jsonPayload = '{"categoryName":"bar", "activeStatus":true}'

    void setup() {
        validatorFactory = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test -- JSON serialization to Category'() {
        when:
        Category category = mapper.readValue(jsonPayload, Category)

        then:
        category.categoryName == "bar"
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Category with invalid payload'() {
        when:
        mapper.readValue(payload, Category)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                    | exceptionThrown          | message
        'non-jsonPayload'          | JsonParseException       | 'Unrecognized token'
        '[]'                       | MismatchedInputException | 'Cannot deserialize value of type'
        '{categoryName: "test"}'   | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}'  | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    void 'test JSON deserialization to Category object - categoryName is empty'() {
        given:
        String jsonPayloadBad = '{"categoryMissing":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayloadBad, Category)

        then:
        category.categoryName.empty
        0 * _
    }

    void 'test validation valid category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.categoryName = "foobar"

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.empty
    }

    void 'test validation invalid category - empty categoryName'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.size() == 1
    }
}
