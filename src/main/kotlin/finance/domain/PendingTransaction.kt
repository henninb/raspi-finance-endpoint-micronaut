package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.ValidDate
import jakarta.persistence.*
import jakarta.persistence.GenerationType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

@Entity
@Table(name = "t_pending_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PendingTransaction(
    @field:Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_transaction_id")
    @param:Min(value = 0L)
    @param:JsonProperty
    var pendingTransactionId: Long = 0L,

    @param:JsonProperty
    @param:Size(min = 3, max = 40)
    @param:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    var accountNameOwner: String,

    @param:JsonProperty
    @field:ValidDate
    @Column(name = "transaction_date", nullable = false, columnDefinition = "DATE")
    var transactionDate: Date,

    @param:JsonProperty
    @param:Size(min = 1, max = 75)
    @param:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    var description: String,

    @param:JsonProperty
    @param:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var amount: BigDecimal,

    @param:JsonProperty
    @Column(name = "review_status", nullable = false, columnDefinition = "TEXT DEFAULT 'pending'")
    var reviewStatus: String = "pending",

    @param:JsonProperty
    @Column(name = "owner", nullable = true)
    var owner: String? = null
) {

    constructor() : this(0L, "", Date(0),"",BigDecimal(0.00), "pending", "")

    @JsonSetter("transactionDate")
    fun jsonSetterPaymentDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}