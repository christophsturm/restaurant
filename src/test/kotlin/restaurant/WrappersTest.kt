package restaurant

import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Testable
class WrappersTest {
    val context = describe("Wrappers") {
        val wrapper = mock<Wrapper>()
        val restaurant = autoClose(Restaurant {
            wrap(wrapper) {
                post("/handlers/reverser", ReverserService())
            }
        })
        val response = request(restaurant, "/handlers/reverser") {
            post("""jakob""".toRequestBody())
        }
        it("calls the wrapped handler") {
            expectThat(response) {
                get { code }.isEqualTo(201)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }
        }
        pending("calls the wrapper") {
            expectThat(getCalls(wrapper)).containsExactly(call(Wrapper::invoke))
        }

    }

}
