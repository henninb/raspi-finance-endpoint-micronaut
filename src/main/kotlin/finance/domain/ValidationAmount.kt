package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.TransactionStateConverter
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min

@MappedEntity("t_validation_amount")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ValidationAmount(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.IDENTITY)
    @field:Min(value = 0L)
    @param:JsonProperty
    var validationId: Long,

    @param:JsonProperty
    @field:Min(value = 0L)
    var accountId: Long,

    //@field:ValidTimestamp
    @param:JsonProperty
    var validationDate: Timestamp,

    @param:JsonProperty
    var activeStatus: Boolean = true,

    @param:JsonProperty
    var transactionState: TransactionState,

    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    var amount: BigDecimal
) {
    constructor() : this(0L, 0L, Timestamp(0L), true, TransactionState.Undefined, BigDecimal(0.0))

    @JsonIgnore
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}