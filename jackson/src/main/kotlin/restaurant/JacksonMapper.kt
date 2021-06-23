package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import restaurant.internal.Mapper

class JacksonMapper(private val jackson: ObjectMapper) : Mapper {
    override fun <T : Any> readValue(requestBody: ByteArray?, clazz: Class<T>): T =
        jackson.readValue(requestBody, clazz)

    override fun writeValueAsBytes(value: Any?): ByteArray = jackson.writeValueAsBytes(value)
}
