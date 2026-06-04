package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import finance.helpers.PaymentBuilder
import finance.utils.Constants
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import java.time.LocalDate

class PaymentSpec extends Specification {
    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()

    protected String jsonPayload = '{"sourceAccount":"checking_brian","destinationAccount":"foo_brian","amount":5.12,"guidSource":"78f65481-f351-4142-aff6-73e99d2a286d","guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299","transactionDate":"2020-11-12"}'

    void setup() {
        validatorFactory = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test -- JSON deserialization to Payment'() {
        when:
        Payment payment = mapper.readValue(jsonPayload, Payment)

        then:
        payment.sourceAccount == 'checking_brian'
        payment.destinationAccount == 'foo_brian'
        payment.amount == 5.12
        payment.guidSource == '78f65481-f351-4142-aff6-73e99d2a286d'
        payment.guidDestination == '0db56665-0d47-414e-93c5-e5ae4c5e4299'
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Payment with invalid payload'() {
        when:
        mapper.readValue(payload, Payment)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                | exceptionThrown          | message
        'non-jsonPayload'      | JsonParseException       | 'Unrecognized token'
        '[]'                   | MismatchedInputException | 'Cannot deserialize value of type'
        '{guidSource: "test"}' | JsonParseException       | 'was expecting double-quote to start field name'
        '{"amount": "123",}'   | JsonParseException       | 'was expecting double-quote to start field name'
    }

    void 'test validation valid payment'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.empty
    }

    @Unroll
    void 'test validation invalid #invalidField'() {
        given:
        Payment payment = new PaymentBuilder()
                .withSourceAccount(sourceAccount)
                .withDestinationAccount(destinationAccount)
                .withTransactionDate(transactionDate)
                .withAmount(amount)
                .withGuidDestination(guidDestination)
                .withGuidSource(guidSource)
                .build()

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.size() == errorCount

        where:
        invalidField        | sourceAccount    | destinationAccount | transactionDate            | amount | guidDestination              | guidSource                   | errorCount
        'sourceAccount'     | 'a_'             | 'foo_brian'        | LocalDate.of(2020, 10, 15) | 5.0    | UUID.randomUUID().toString() | UUID.randomUUID().toString() | 1
        'guidDestination'   | 'checking_brian' | 'foo_brian'        | LocalDate.of(2020, 10, 16) | 5.0    | 'invalid'                    | UUID.randomUUID().toString() | 1
        'guidSource'        | 'checking_brian' | 'foo_brian'        | LocalDate.of(2020, 10, 17) | 5.0    | UUID.randomUUID().toString() | 'invalid'                    | 1
    }
}
