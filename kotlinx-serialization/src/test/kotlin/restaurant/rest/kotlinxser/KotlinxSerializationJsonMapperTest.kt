package restaurant.rest.kotlinxser

import failgood.Ignored
import failgood.Test
import failgood.testsAbout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class S(val s: String)

@Test
object KotlinxSerializationJsonMapperTest {
    val context =
        testsAbout(
            KotlinxSerializationMapper::class,
            ignored = Ignored.Because("mapper api needs a refactoring before this can be done in a great way")
        ) {
            it("reads value") {
                KotlinxSerializationMapper(Json).readValue("""{"s":"stringVal"}""".toByteArray(), S::class.java)
            }
        }
}
