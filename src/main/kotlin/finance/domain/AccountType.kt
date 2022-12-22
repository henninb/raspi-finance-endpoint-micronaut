package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class AccountType(val type: String) {
    @JsonProperty("credit")
    Credit("credit"),

    @JsonProperty("debit")
    Debit("debit"),

    @JsonProperty("undefined")
    Undefined("undefined");

    //fun value() : String = type
    override fun toString(): String = name.lowercase()

//    fun parseAccountType(str: String): AccountType {
//        return when(str) {
//            "undefined" -> Undefined
//            else -> valueOf(str)
//        }
//    }

//    override fun valueof(): AccountType {
//
//        return Undefined
//    }

    companion object {
        //private val VALUES = values();
        //fun getByValue(type: String) = VALUES.firstOrNull { it.type == type }
        //fun from(type: String?): AccountType = values().find { it.name == type } ?: Undefined
        //operator fun invoke(type: String?): AccountType = values().find { it.name == type } ?: Undefined
        //fun valueOf(type: String): AccountType? = values().find { it.type == type }
        //private val map = AccountType.values().associateBy(AccountType::type)
        //fun valueOf(type: String): AccountType = values().find { it.name == type } ?: Undefined
    }
}