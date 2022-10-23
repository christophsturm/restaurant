package restaurant.rest.kotlinxser

import failgood.Ignored
import failgood.Test
import failgood.describe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import restaurant.internal.Mapper

@Serializable
data class S(val s: String)

@Test
object KotlinxSerializationJsonMapperTest {
    val context =
        describe<KotlinxSerializationMapper>(
            ignored =
            Ignored.Because("mapper api needs a refactoring before this can be done in a great way")
        ) {
            it("reads value") {
                KotlinxSerializationMapper(Json).readValue("""{"s":"stringVal"}""".toByteArray(), S::class.java)
            }
        }
}

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
