package restaurant

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.ByteBuffer
import java.util.Deque

class Reverser : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(request.readBody().reversedArray())))
    }
}

@Testable
class TestabilityTest {
    val context = describe("testability") {
        it("handlers can be tested in isolation") {
            val handler = Reverser()
            expectThat(handler.handle(MockRequest("jakob"), RequestContext()).bodyString()).isEqualTo("bokaj")
        }
    }
}

class MockRequest(private val body: String) : Request {
    override val requestPath: String
        get() = TODO("Not yet implemented")
    override val queryString: String
        get() = TODO("Not yet implemented")
    override val headers: HeaderMap
        get() = TODO("Not yet implemented")
    override val queryParameters: Map<String, Deque<String>>
        get() = TODO("Not yet implemented")

    override suspend fun readBody(): ByteArray = body.toByteArray()

}

fun RequestContext() = object : RequestContext {
    override fun <T> get(key: Key<T>): T {
        TODO("Not yet implemented")
    }
}
