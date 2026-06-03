package finance.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.LocalDate

class FlexibleLocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
        val value = p.text.trim()
        return LocalDate.parse(if (value.length > 10) value.substring(0, 10) else value)
    }
}
