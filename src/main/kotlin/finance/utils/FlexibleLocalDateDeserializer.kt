package finance.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FlexibleLocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
        val value = p.text.trim()
        value.toLongOrNull()?.let { epoch ->
            val instant = if (value.length > 10) Instant.ofEpochMilli(epoch) else Instant.ofEpochSecond(epoch)
            return instant.atZone(ZoneOffset.UTC).toLocalDate()
        }
        return LocalDate.parse(if (value.length > 10) value.substring(0, 10) else value)
    }
}
