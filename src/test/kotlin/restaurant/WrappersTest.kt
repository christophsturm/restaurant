package restaurant

import failgood.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Testable
class WrappersTest {
    val context = describe("Wrapper Support") {
        val events = mutableListOf<String>()
        val inner = object : Wrapper {
            override suspend fun invoke(exchange: Exchange): StringResponse? {
                events.add("inner")
                return null
            }
        }
        val outer = object : Wrapper {
            override suspend fun invoke(exchange: Exchange): StringResponse? {
                events.add("outer")
                return null
            }
        }
        val restaurant = autoClose(Restaurant {
            wrap(outer) {
                wrap(inner) {
                    post("/handlers/reverser", ReverserService())
                }
            }
        })
        val response = request(restaurant, "/handlers/reverser") {
            post("""jakob""".toRequestBody())
        }
        it("calls the wrapped handler") {
            expectThat(response) {
                get { code }.isEqualTo(200)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }
        }
        it("calls the wrappers") {
            expectThat(events).containsExactly("outer", "inner")
        }
    }
}
