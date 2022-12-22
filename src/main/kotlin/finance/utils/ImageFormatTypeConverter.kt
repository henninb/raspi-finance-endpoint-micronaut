package finance.utils

import finance.domain.ImageFormatType
import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.util.*

@Converter
class ImageFormatTypeConverter : AttributeConverter<ImageFormatType, String> {

    override fun convertToDatabaseColumn(attribute: ImageFormatType): String {
        return when (attribute) {
            ImageFormatType.Jpeg -> "jpeg"
            ImageFormatType.Png -> "png"
            ImageFormatType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): ImageFormatType {
        return when (attribute.trim().lowercase(Locale.getDefault())) {
            "jpeg" -> ImageFormatType.Jpeg
            "png" -> ImageFormatType.Png
            "undefined" -> ImageFormatType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
