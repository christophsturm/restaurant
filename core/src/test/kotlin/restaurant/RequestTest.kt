package restaurant

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class RequestTest {
    val context = describe(Request::class) {
        lateinit var exch: Request

        class TestHandler : SuspendingHandler {
            override suspend fun handle(request: Request, requestContext: RequestContext): Response {
                exch = request
                return response(200)
            }
        }

        val restaurant = autoClose(
            Restaurant {
                route(Method.GET, "/path", TestHandler())
            }
        )

        val response = request(restaurant, "/path?query=string")
        expectThat(response).get { code }.isEqualTo(200)
        it("exposes the query string") {
            expectThat(exch.queryString).isEqualTo("query=string")
        }
        it("exposes the request path") {
            expectThat(exch.requestPath).isEqualTo("/path")
        }

    }
}

