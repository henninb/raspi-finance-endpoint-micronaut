package finance.utils

import finance.domain.TransactionType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.Locale

@Converter
class TransactionTypeConverter : AttributeConverter<TransactionType, String> {

    override fun convertToDatabaseColumn(attribute: TransactionType): String = attribute.label

    override fun convertToEntityAttribute(dbData: String): TransactionType =
        when (dbData.trim().lowercase(Locale.getDefault())) {
            "expense" -> TransactionType.Expense
            "income" -> TransactionType.Income
            "transfer" -> TransactionType.Transfer
            "undefined" -> TransactionType.Undefined
            else -> throw RuntimeException("Unknown TransactionType: $dbData")
        }
}
