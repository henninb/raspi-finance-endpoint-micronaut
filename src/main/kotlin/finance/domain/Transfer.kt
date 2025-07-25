package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.LowerCaseConverter
import finance.utils.ValidDate
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import jakarta.persistence.*
import jakarta.persistence.GenerationType
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Table(name = "t_transfer")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transfer(
    @field:Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @param:Min(value = 0L)
    @param:JsonProperty
    var transferId: Long,

    @param:JsonProperty
    @param:Size(min = 3, max = 40)
    @param:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var sourceAccount: String,

    @param:JsonProperty
    @param:Size(min = 3, max = 40)
    @param:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var destinationAccount: String,

    @field:ValidDate
    @param:JsonProperty
    var transactionDate: Date,

    @param:JsonProperty
    @param:Digits(integer = 8, fraction = 2, message = Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    var amount: BigDecimal,

    @param:JsonProperty
    @param:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    var guidSource: String?,

    @param:JsonProperty
    @param:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    var guidDestination: String?,

    @param:JsonProperty
    var activeStatus: Boolean = true
) {

    constructor() : this(0L, "", "", Date(0), BigDecimal(0.00), "", "")

    @JsonGetter("transactionDate")
    fun jsonGetterTransferDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        return simpleDateFormat.format(this.transactionDate)
    }

    @JsonSetter("transactionDate")
    fun jsonSetterTransfertDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }

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