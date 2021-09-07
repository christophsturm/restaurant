package restaurant

import failgood.Test
import failgood.describe
import restaurant.test.MockRequest
import restaurant.test.RequestContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.ByteBuffer

class Reverser : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(request.withBody().body!!.reversedArray())))
    }
}

@Test
class TestabilityTest {
    val context = describe("testability") {
        it("handlers can be tested in isolation") {
            val handler = Reverser()
            expectThat(
                handler.handle(MockRequest("jakob".toByteArray()), RequestContext()).bodyString()
            ).isEqualTo("bokaj")
        }
    }
}

