package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import jakarta.persistence.*
import java.sql.Timestamp
import java.util.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Entity
@Table(name = "t_parameter")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Parameter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_parameter_parameter_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "parameter_id", nullable = false)
    var parameterId: Long,

    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_name", unique = true, nullable = false)
    @JsonProperty
    var parameterName: String,

    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_value", nullable = false)
    @JsonProperty
    var parameterValue: String,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @JsonProperty
    @Column(name = "owner", nullable = true)
    var owner: String? = null
) {
    constructor() : this(0L, "", "", true, null)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
