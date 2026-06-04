package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.utils.Constants
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import spock.lang.Specification

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator

class BaseServiceSpec extends Specification {
    protected AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected ReceiptImageRepository receiptImageRepositoryMock = GroovyMock(ReceiptImageRepository)
    protected CategoryRepository categoryRepositoryMock = GroovyMock(CategoryRepository)
    protected DescriptionRepository descriptionRepositoryMock = GroovyMock(DescriptionRepository)
    protected PaymentRepository paymentRepositoryMock = GroovyMock(PaymentRepository)
    protected ParameterRepository parameterRepositoryMock = GroovyMock(ParameterRepository)
    protected TransactionRepository transactionRepositoryMock = GroovyMock(TransactionRepository)
    protected ObjectMapper mapper = new ObjectMapper()
    protected Validator validator = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator()
//    protected String baseName = new FileSystemResource("").file.absolutePath
    protected Counter counter = Mock(Counter)
    protected DescriptionService descriptionService = new DescriptionService(descriptionRepositoryMock, transactionRepositoryMock, validatorMock, meterService)
    protected AccountService accountService = new AccountService(accountRepositoryMock, transactionRepositoryMock, validatorMock, meterService)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(receiptImageRepositoryMock, validatorMock, meterService)
    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, transactionRepositoryMock, validatorMock, meterService)
    protected CalculationService calculationService = new CalculationService(transactionRepositoryMock, meterService)
    protected TransactionService transactionService = new TransactionService(transactionRepositoryMock, accountService, categoryService, descriptionService, receiptImageService, validatorMock, meterService, calculationService)
    protected ParameterService parameterService = new ParameterService(parameterRepositoryMock, validatorMock, meterService)
    protected PaymentService paymentService = new PaymentService(paymentRepositoryMock, transactionService, accountService, validatorMock, meterService)

    //TODO: turn this into a method
    protected Tag validationExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'ValidationException')
    protected Tag runtimeExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'RuntimeException')
    protected Tag exceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'Exception')
    protected Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'localhost')
    protected Tags validationExceptionTags = Tags.of(validationExceptionTag, serverNameTag)
    protected Tags runtimeExceptionTags = Tags.of(runtimeExceptionTag, serverNameTag)
    protected Meter.Id validationExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, validationExceptionTags, null, null, Meter.Type.COUNTER)
    protected Meter.Id runtimeExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, runtimeExceptionTags, null, null, Meter.Type.COUNTER)
    protected Meter.Id runtimeExceptionCaughtMeter = new Meter.Id(Constants.EXCEPTION_CAUGHT_COUNTER, runtimeExceptionTags, null, null, Meter.Type.COUNTER)

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'localhost')
        Tag accountNameOwnerTag = Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }
}
