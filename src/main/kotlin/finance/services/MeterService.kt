package finance.services

import finance.utils.Constants
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterService(@Inject val meterRegistry: MeterRegistry) {

    private val hostName = "server" //setHostName()

    fun setHostName(): String? {
        val env = System.getenv()
        return if (env.containsKey("COMPUTERNAME")) env["COMPUTERNAME"] else if (env.containsKey("HOSTNAME")) env["HOSTNAME"] else "Unknown"
    }

    fun incrementExceptionThrownCounter(exceptionName: String): Unit = Counter
        .builder(Constants.EXCEPTION_THROWN_COUNTER)
        .description("Increments the counter for every exception thrown.")
        .tags(
            listOfNotNull(
                Tag.of(Constants.EXCEPTION_NAME_TAG, exceptionName),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementExceptionCaughtCounter(exceptionName: String): Unit = Counter
        .builder(Constants.EXCEPTION_CAUGHT_COUNTER)
        .description("Increments the counter for every exception caught.")
        .tags(
            listOfNotNull(
                Tag.of(Constants.EXCEPTION_NAME_TAG, exceptionName),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionUpdateClearedCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER)
        .description("Increments the counter for each transaction state toggled to cleared.")
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionSuccessfullyInsertedCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionAlreadyExistsCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionRestSelectNoneFoundCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionRestTransactionStateUpdateFailureCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionRestReoccurringStateUpdateFailureCounter(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementAccountListIsEmpty(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementTransactionReceiptImageInserted(accountNameOwner: String): Unit = Counter
        .builder(Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER)
        .tags(
            listOfNotNull(
                Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                Tag.of(Constants.SERVER_NAME_TAG, hostName),
            )
        )
        .register(meterRegistry)
        .increment()

    fun incrementCamelStringProcessor(accountNameOwner: String): Unit {
        Counter
            .builder(Constants.CAMEL_STRING_PROCESSOR_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(Constants.SERVER_NAME_TAG, hostName),
                )
            )
            .register(meterRegistry)
            .increment()
    }

    fun incrementCamelTransactionSuccessfullyInsertedCounter(accountNameOwner: String): Unit {
        Counter
            .builder(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(Constants.SERVER_NAME_TAG, hostName),
                )
            )
            .register(meterRegistry)
            .increment()
    }
}