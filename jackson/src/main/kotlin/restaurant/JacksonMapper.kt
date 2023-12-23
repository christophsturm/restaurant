package restaurant

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import restaurant.HttpStatus.BAD_REQUEST_400
import restaurant.internal.Mapper

class JacksonMapper(
    private val jackson: ObjectMapper = jacksonObjectMapper().configure(
        StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(),
        true
    )
) : Mapper {
    override fun <T : Any> readValue(requestBody: ByteArray, clazz: Class<T>): T =
        try {
            jackson.readValue(requestBody, clazz)
        } catch (e: JsonProcessingException) {
            throw BadRequestException(e)
        }

    override fun writeValueAsBytes(value: Any): ByteArray = jackson.writeValueAsBytes(value)
    override fun writeValueAsString(value: Any): String = jackson.writeValueAsString(value)
}

class BadRequestException(e: JsonProcessingException) :
    ResponseException(response(BAD_REQUEST_400, e.message!!), e.message)
