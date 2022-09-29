package restaurant

import failgood.Test
import failgood.describe
import restaurant.test.MockRequest
import restaurant.test.RequestContext
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.ByteBuffer

class Reverser : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(request.withBody().body!!.reversedArray())))
    }
}

@Test
class TestabilityTest {
    val context = describe("testability") {
        test("handlers can be invoked with a MockRequest") {
            val handler = Reverser()
            expectThat(
                handler.handle(MockRequest("jakob".toByteArray()), RequestContext()).bodyString()
            ).isEqualTo("bokaj")
        }
        describe("the mock request") {
            it("has a body") {
                expectThat(MockRequest("jakob".toByteArray()).withBody()).get { body }.isNotNull().get { String(this) }
                    .isEqualTo("jakob")
            }
            it("has query parameters") {
                expectThat(
                    MockRequest(
                        queryParameters = mapOf("key" to listOf("value1", "value2"))
                    ).withBody()
                ).get { queryParameters["key"] }.isNotNull().containsExactly("value1", "value2")
            }
        }
    }
}
