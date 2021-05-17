package restaurant

import failfast.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Testable
class WrappersTest {
    val context = describe("Wrappers") {
        val restaurant = autoClose(Restaurant {
            wrap(HeaderWrapper()) {
                post("/handlers/reverser", ReverserService())
            }
        })
        it("calls the wrapped handler") {
            val response = request(restaurant, "/handlers/reverser") {
                post("""jakob""".toRequestBody())
            }
            expectThat(response) {
                get { code }.isEqualTo(201)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }

        }
    }

}

class HeaderWrapper : Wrapper {

}
