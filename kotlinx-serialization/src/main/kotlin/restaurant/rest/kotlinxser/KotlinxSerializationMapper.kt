package restaurant.rest.kotlinxser

import kotlinx.serialization.json.Json
import restaurant.internal.Mapper

class KotlinxSerializationMapper(private val json: Json) : Mapper {
    override fun <T : Any> readValue(requestBody: ByteArray, clazz: Class<T>): T {
        TODO("Not yet implemented")
    }

    override fun writeValueAsBytes(value: Any): ByteArray {
        TODO("Not yet implemented")
    }

    override fun writeValueAsString(value: Any): String {
        TODO("Not yet implemented")
    }
}
