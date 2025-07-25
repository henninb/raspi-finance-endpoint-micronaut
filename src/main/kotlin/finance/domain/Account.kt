package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.LowerCaseConverter
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Calendar
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Entity
@Table(
    name = "t_account",
    uniqueConstraints = [UniqueConstraint(
        columnNames = ["account_name_owner", "account_type"],
        name = "uk_id_account_type"
    )]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_account_account_id_seq")
    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @JsonProperty
    @Column(name = "account_name_owner", unique = true, nullable = false)
    @field:Size(min = 3, max = 40)
    @field:Convert(converter = LowerCaseConverter::class)
    //@field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var accountNameOwner: String,

    @JsonProperty
    @Column(name = "account_type", nullable = false)
    @Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @JsonProperty
    //@field:Pattern(regexp = "^[0-9]{4}$", message = FIELD_MUST_BE_FOUR_DIGITS_MESSAGE)
    @Column(name = "moniker", columnDefinition = "TEXT DEFAULT '0000'")
    var moniker: String,

    @JsonProperty
    //@field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "outstanding", precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var outstanding: BigDecimal,

    @JsonProperty
    //@field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "future", precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var future: BigDecimal,

    @JsonProperty
    //@field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "cleared", precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var cleared: BigDecimal,

    @JsonProperty
    @Column(name = "payment_required", nullable = true, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var paymentRequired: Boolean? = true,

    @JsonProperty
    @Column(name = "account_name", nullable = true)
    var accountName: String? = null,

    @JsonProperty
    @Column(name = "account_owner", nullable = true)
    var accountOwner: String? = null,

    @JsonProperty
    @Column(name = "owner", nullable = true)
    var owner: String? = null,

) {
    constructor() : this(
        0L, "", AccountType.Undefined, true,
        "0000", BigDecimal(0.0), BigDecimal(0.0), BigDecimal(0.0),
        true, null, null, null
    )

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "validation_date", nullable = false)
    var validationDate: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_closed", nullable = false)
    var dateClosed: Timestamp = Timestamp(0)

    override fun toString(): String {
        //mapper.setTimeZone(TimeZone.getDefault())
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}