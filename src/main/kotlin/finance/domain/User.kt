package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.persistence.GenerationType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Entity
@Table(name = "t_user")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
        @field:Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @param:Min(value = 0L)
        @param:JsonProperty
        var userId: Long,

        @param:JsonProperty
        var activeStatus: Boolean = true,

        @field:Size(min = 1, max = 40)
        @get:JsonProperty
        var firstName: String,

        @field:Size(min = 1, max = 40)
        @get:JsonProperty
        var lastName: String,

        @field:Size(min = 1, max = 60)
        @param:JsonProperty
        var username: String,

        @field:Size(min = 1, max = 60)
        @param:JsonProperty
        var password: String
) {
    constructor() : this(0L, true, "","", "","")

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