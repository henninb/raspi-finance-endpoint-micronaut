package finance.utils

import org.apache.logging.log4j.LogManager
import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.util.*

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String {
        if (attribute == null) {
            return ""
        }
        logger.debug("convertToDatabaseColumn - converted to lowercase")
        return attribute.lowercase(Locale.getDefault())
    }

    override fun convertToEntityAttribute(attribute: String?): String {
        if (attribute == null) {
            return ""
        }

        logger.debug("convertToEntityAttribute - converted to lowercase")
        return attribute.lowercase(Locale.getDefault())
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
