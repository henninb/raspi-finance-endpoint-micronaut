package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.ValidDate
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@MappedEntity("t_pending_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PendingTransaction(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.IDENTITY)
    @param:Min(value = 0L)
    @param:JsonProperty
    var pendingTransactionId: Long = 0L,

    @param:JsonProperty
    @param:Size(min = 3, max = 40)
    @param:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var accountNameOwner: String,

    @param:JsonProperty
    @field:ValidDate
    var transactionDate: Date,

    @param:JsonProperty
    @param:Size(min = 1, max = 75)
    @param:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    var description: String,

    @param:JsonProperty
    @param:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    var amount: BigDecimal,

    @param:JsonProperty
    var reviewStatus: String = "pending",

    @param:JsonProperty
    var owner: String? = null,

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    @field:JsonIgnore
    var account: Account? = null
) {

    constructor() : this(0L, "", Date(0),"",BigDecimal(0.00), "pending", "")

    @JsonSetter("transactionDate")
    fun jsonSetterPaymentDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }

    @JsonIgnore
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}