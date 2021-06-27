package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import restaurant.internal.Mapper

class JacksonMapper(private val jackson: ObjectMapper = jacksonObjectMapper()) : Mapper {
    override fun <T : Any> readValue(requestBody: ByteArray?, clazz: Class<T>): T =
        jackson.readValue(requestBody, clazz)

    override fun writeValueAsBytes(value: Any?): ByteArray = jackson.writeValueAsBytes(value)
}
