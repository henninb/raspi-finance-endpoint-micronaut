package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.AccountBuilder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

class AccountSpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    @Shared
    protected String jsonPayload = '''
{"accountNameOwner":"discover_brian","accountType":"credit","activeStatus":true,
"moniker":"1234","totals":0.01,"totalsBalanced":0.02,
"dateClosed":0}
'''

    @Shared
    protected String jsonPayloadInvalidAccountType = '''
{"accountNameOwner":"discover_brian","accountType":"non-valid","activeStatus":true,
"moniker":"1234","totals":0.01,"totalsBalanced":0.02,
"dateClosed":0,"dateUpdated":1553645394000,"dateAdded":1553645394000}
'''

    void 'test JSON deserialization to Account'() {
        when:
        Account account = mapper.readValue(jsonPayload, Account)

        then:
        account.accountType == AccountType.Credit
        account.accountNameOwner == "discover_brian"
        0 * _
    }

    void 'test validation valid account'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        Set<ConstraintViolation<Account>> violations = validator.validate(account)

        then:
        violations.empty
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Account with invalid payload'() {
        when:
        mapper.readValue(payload, Account)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                       | exceptionThrown          | message
        'non-jsonPayload'             | JsonParseException       | 'Unrecognized token'
        '[]'                          | MismatchedInputException | 'Cannot deserialize value of type'
        '{accountNameOwner: "test"}'  | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}'     | InvalidFormatException   | 'Cannot deserialize value of type'
        jsonPayloadInvalidAccountType | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    @Unroll
    void 'test validation invalid #invalidField has error #expectedError'() {
        given:
        Account account = new AccountBuilder()
                .withAccountType(accountType)
                .withMoniker(moniker)
                .withAccountNameOwner(accountNameOwner)
                .withActiveStatus(activeStatus)
                .withTotals(totals)
                .withTotalsBalanced(totalsBalanced)
                .build()

        when:
        Set<ConstraintViolation<Account>> violations = validator.validate(account)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == account.properties[invalidField]

        where:
        invalidField       | accountType        | accountNameOwner | moniker | activeStatus | totals | totalsBalanced | expectedError                   | errorCount
        'accountNameOwner' | AccountType.Credit | '_b'             | '0000'  | true         | 0.00G  | 0.00G          | 'size must be between 3 and 40' | 1
    }
}
